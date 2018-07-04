#!/usr/bin/perl


# Copyright Copyright 2009-15-10 Simon Andrews
#
#    This file is part of SeqMonk.
#
#    SeqMonk is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 3 of the License, or
#    (at your option) any later version.
#
#    SeqMonk is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with SeqMonk; if not, write to the Free Software
#    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

use warnings;
use strict;
use lib ("/home/andrewss/EnsemblAPI/bioperl-live");
#use lib ("/home/andrewss/EnsemblAPI/plants/ensembl/modules");
use lib ("/home/andrewss/EnsemblAPI/ensembl/modules");
use Bio::EnsEMBL::Registry;
use Bio::Seq::RichSeq;
use Bio::SeqIO;
use Bio::SeqFeature::Generic;
use Bio::Location::Split;
use Bio::Location::Simple;
use Bio::EnsEMBL::DBSQL::OntologyDBAdaptor;
use Bio::EnsEMBL::ApiVersion;


$|++;

system("clear") == 0 or warn "Couldn't clear screen";

my $version = software_version();

my $registry = load_registry();

my $GO_adapter =   $registry->get_adaptor( 'Multi', 'Ontology', 'GOTerm' );

die "Couldn't get GO adaptor" unless ($GO_adapter);

my $species = $ARGV[0];

if ($species) {
  my $db_adapter = $registry->get_DBAdaptor($species,'Core');
  process_genome($db_adapter);
}
else {
  select_db_adapter($registry);
}

sub load_registry {

  warn "Loading Registry information - please be patient\n";

  my $registry = 'Bio::EnsEMBL::Registry';

  $registry->load_registry_from_db(
				 -host => 'mysql-eg-publicsql.ebi.ac.uk',
				 -port => 4157,
				 -user => 'anonymous'
				);

#  $registry -> set_disconnect_when_inactive(1);

  my $ontology_dba = Bio::EnsEMBL::DBSQL::OntologyDBAdaptor -> new (
								    -SPECIES => 'Multi',
								    -GROUP => 'ontology',
								    -HOST => 'mysql-eg-publicsql.ebi.ac.uk',
								    -PORT => 4157,
								    -USER => 'anonymous',
								    -DBNAME => 'ensemblgenomes_ontology_34_87',
								   );

  return $registry;

}

sub select_db_adapter {

  my $registry = shift;

  my @adapters = @{$registry -> get_all_DBAdaptors(-group => 'core')};

  warn "Found ".scalar @adapters." adapters from $registry \n";

  my @slice_adapters;

  my $count = 0;

  foreach my $adapter (@adapters) {
    ++$count;
    #warn "$count:Found adapter of type ".$adapter->group()."\n";

    # We'll want to know the name of the current assembly which
    # we fetch via a coordsystem adapter
    next unless ($adapter->get_adaptor('coordsystem')->fetch_all()->[0]->version());

    warn $adapter->get_adaptor('coordsystem')->fetch_all()->[0]->version(),"\n";

    push @slice_adapters,$adapter;
    $adapter->dbc()->disconnect_if_idle();
  }

  @slice_adapters = sort {$a->species() cmp $b->species()} @slice_adapters;

#  foreach my $adapter (@slice_adapters) {
#    if ($adapter->species() =~ /^\w_/) {
#      process_genome($adapter);
#    }
#    $adapter->dbc()->disconnect_if_idle();
#  }
#
#  die "Not interactive this time";
  while (1) {

    print "Select assembly to process\n--------------------------\n";

    print "0)   Exit\n";


    for (0..$#slice_adapters) {

      my $adapter = $slice_adapters[$_];


      print 
	$_+1,")",
	  (" "x(4-length($_+1))),
	$adapter->species(),
	  (" "x(30-length($adapter->species()))),$adapter->get_adaptor('coordsystem')->fetch_all->[0]->version(),
	    "\n";

      $adapter->dbc()->disconnect_if_idle();

    }

    print "Species (separate multiple with commas)>";

    my $species_choice = <STDIN>;
    chomp $species_choice;

    my @species = split(/\s*,\s*/,$species_choice);

    foreach my $species (@species) {

      if ($species !~ /^\d+$/) {
	print "$species wasn't a number!\n";
	next;
      }

      if ($species == 0) {
	print "Thanks for playing...\n";
	exit;
      }

      $species -= 1;

      if ($species < 0 or $species > $#slice_adapters) {
	print "That wasn't one of the choices\n";
	next;
      }

      process_genome($slice_adapters[$species]);
    }
  }

}


sub process_genome {

  my $db_adapter = shift;

  warn "Processing genome ".$db_adapter->species()."\n";

  my $assembly = $db_adapter->get_adaptor('coordsystem')->fetch_all->[0]->version();

  $assembly .= "_v$version";

  my $species = $db_adapter->species();

  my $readable_species = $species;
  $readable_species =~ s/_/ /g;
  $readable_species =~ s/^(\w)/uc $1/e;

  # Find a suitable directory for this species
  if (! -e $readable_species) {
    mkdir ($readable_species) or die "Can't create directory for '$readable_species': $!";
  }
  chdir ($readable_species) or die "Can't move into directory for '$readable_species': $!";

  if (-e $assembly || -e "$assembly.zip") {
    warn "There's already a folder for $assembly\n";
    chdir("..") or die $!;
    return;
  }

  mkdir ($assembly) or die "Can't make directory for assmebly '$assembly':$!";
  chdir ($assembly) or die "Can't move into directory for assmebly '$assembly':$!";


  # In some Ensembl species there are duplicated regions (eg the PAR in chrs X/Y).
  # If you want to see the full lengths of chromosomes then you need to pass in
  # the 4 argument form of fetch_all.  Specifying just 'chromosome' gets us separate
  # regions for the non-duplicated parts of duplicated chromosomes.

  my @chr_slices = @{$db_adapter -> get_adaptor('slice') -> fetch_all('chromosome',undef,0,1)};
  my @chr_group_slices = @{$db_adapter -> get_adaptor('slice') -> fetch_all('chromosome_group',undef,0,1)};
  my @plasmid_slices = @{$db_adapter -> get_adaptor('slice') -> fetch_all('plasmid',undef,0,1)};

  # Destroy the chr array as we're iterating since
  # the API does lazy loading which will make these
  # objects balloon in size as we process them.

  while (@plasmid_slices) {

    my $chr_slice = shift @plasmid_slices;

    process_chromosome($chr_slice,$readable_species,$assembly);
  }


  while (@chr_slices) {

    my $chr_slice = shift @chr_slices;

    process_chromosome($chr_slice,$readable_species,$assembly);
  }

  while (@chr_group_slices) {

    my $chr_slice = shift @chr_group_slices;

    process_chromosome($chr_slice,$readable_species,$assembly);
  }

  chdir("../..") or die "Can't move back out of $readable_species/$assembly directory: $!";


  # The -m deletes the source files after creating the archive.
  system("zip -r '${assembly}.zip' '$readable_species'") == 0 or die "Can't create zip file from '$readable_species'";

  rename("$assembly.zip","$readable_species/$assembly.zip") or die "Can't move zip file to species directory: $!";

  system("rm -r '$readable_species/$assembly'") == 0 or die "Can't delete uncompressed data: $!";

}


sub process_chromosome {

  my $chr_slice = shift;
  my $species = shift;
  my $assembly = shift;

#  if (length($chr_slice->seq_region_name()) > 5) {
#    warn "Skipping odd looking chromsome ".$chr_slice->seq_region_name()."\n";
#    return;
#  }

  return if ($chr_slice->seq_region_name() =~ /_random/);
  return if ($chr_slice->seq_region_name() eq 'UNKNOWN');

  warn "Processing chromsosome ".$chr_slice->seq_region_name()."(".$chr_slice->length()." bp) \n";

  ###### Testing only ######
  #  return unless ($chr_slice->seq_region_name() eq 'Y');
  ##########################

  my $seq = Bio::Seq::RichSeq -> new(-seq => 'N' x $chr_slice->length(),
				     -id => join(":",($assembly,$chr_slice->seq_region_name())),
				     -accession_number => join(":",("chromosome",$assembly,$chr_slice->seq_region_name(),1,$chr_slice->length(),1)),
				     -molecule => 'DNA',
				    );

  my @genes = @{$chr_slice -> get_all_Genes()};

  while (@genes) {
    my $gene = shift @genes;
    process_gene($seq,$gene);
  }

  my $out = Bio::SeqIO->new (-file => ">".$chr_slice->seq_region_name().".txt",
			     -format => 'EMBL',
			    );

  warn "Writing raw EMBL file\n";
  $out->write_seq($seq);
  warn "Removing sequence\n";
  open (IN,$chr_slice->seq_region_name().".txt") or die "Can't read raw EMBL file: $!";
  open (OUT,'>',$chr_slice->seq_region_name().".dat") or die "Can't write trimmed EMBL file: $!";

  while (<IN>) {
    print OUT;
    last if (/^SQ/);
  }

  close (OUT) or die "Can't write trimmed EMBL file: $!";
  unlink ($chr_slice->seq_region_name().".txt") or die "Can't unlink raw EMBL file: $!";

}

sub process_gene {
  my $seq = shift;
  my $gene = shift;

  unless ($gene->external_name()) {
    $gene->external_name($gene->stable_id());
  }

#  warn "Found gene ".$gene->external_name()." of type ".$gene->biotype()."\n";

  my $genetype = 'gene';

  if ($gene->biotype() eq 'pseudogene') {
    $genetype = 'pseudogene';
  }


  my $feature = new Bio::SeqFeature::Generic (-primary => $genetype,
					      -start => $gene->start(),
					      -end => $gene->end(),
					      -strand => $gene->strand(),
					      -tag => {
						       id => $gene->stable_id(),
						       name => $gene->external_name(),
						       source => $gene->external_db(),
						      }
					     );

  if ($gene->description) {
    $feature -> add_tag_value(description => $gene->description());
  }


  $seq->add_SeqFeature($feature);


  my @xrefs = @{$gene->get_all_DBEntries()};

  while (@xrefs) {
    my $xref = shift @xrefs;
    next if ($xref->dbname() eq $gene->external_db());

    $feature -> add_tag_value(db_xref => $xref->dbname().":".$xref->display_id());
#    warn "Xref of ".$xref->display_id()." in ".$xref->dbname()."\n";
  }

  my @transcripts = @{$gene -> get_all_Transcripts()};

  while (@transcripts) {
    my $transcript = shift @transcripts;


    if ($gene -> biotype() !~ /pseudogene/) {
      process_transcript($seq,$transcript,$gene);
    }
  }

}

sub process_transcript {

  my $seq = shift;
  my $transcript = shift;
  my $gene = shift;

  unless ($transcript->external_name()) {
    $transcript->external_name($gene->external_name());
  }

#  warn "Found transcript ".$transcript->external_name()." of type ".$transcript->biotype()."\n";

  my $type = $transcript->biotype();

  my %other_types = (
		     Mt_tRNA => 'tRNA',
		     Mt_rRNA => 'rRNA',
		     IG_C_gene => 'IG_segment',
		     IG_J_gene => 'IG_segment',
		     IG_V_gene => 'IG_segment',
		     IG_D_gene => 'IG_segment',
		     miRNA => 'miRNA',
		     misc_RNA => 'misc_RNA',
		     snoRNA => 'snoRNA',
		     snRNA => 'snRNA',
		     rRNA => 'rRNA',
		    );

  if (exists $other_types{$type}) {
    $type = $other_types{$type};
  }
  else {
    $type = 'mRNA';
  }

  # Now work out the location from the exons.
  my @exons = @{$transcript -> get_all_Exons()};

  my $location;

  if (@exons == 1) {
    my $exon = shift @exons;
    $location = new Bio::Location::Simple(-start => $exon->start(),
					  -end => $exon->end(),
					  -strand => $exon->strand(),
					 );
  }

  else {
    $location = new Bio::Location::Split();
    while (@exons) {
      my $exon = shift @exons;
      $location->add_sub_Location (
				   new Bio::Location::Simple(-start => $exon->start(),
							     -end => $exon->end(),
							     -strand => $exon->strand(),
							    )
				  );
    }
  }

  my $feature = new Bio::SeqFeature::Generic (-primary => $type,
					      -location => $location,
					      -tag => {
						       id => $transcript->stable_id(),
						       name => $transcript->external_name(),
						       source => $transcript->external_db(),
						      }
					     );

  # Transcripts never seem to have a description
  # so we take the one from the gene instead.
  if ($gene->description()) {
    $feature -> add_tag_value(description => $gene->description());
  }

  $seq->add_SeqFeature($feature);


  my @xrefs = @{$transcript->get_all_DBEntries()};

  while (@xrefs) {
    my $xref = shift @xrefs;
    next unless ($xref->dbname());
    next if ($transcript->external_db() and $xref->dbname() eq $transcript->external_db());
    $feature -> add_tag_value(db_xref => $xref->dbname().":".$xref->display_id());
#    warn "Xref of ".$xref->display_id()." in ".$xref->dbname()."\n";
  }


  process_translation($seq,$transcript,$gene);
}


sub process_translation {

  my $seq = shift;
  my $transcript = shift;
  my $gene = shift;
  my $translation = $transcript->translation();

  # Now see if we have a translation
  my @translateable_exons = @{$transcript->get_all_translateable_Exons()};
  my $location;

  if (@translateable_exons == 0) {
    # No translation
    return;
  }

  elsif (@translateable_exons == 1) {
    my $exon = shift @translateable_exons;
    $location = new Bio::Location::Simple(-start => $exon->start(),
					  -end => $exon->end(),
					  -strand => $exon->strand(),
					 );
  }
  else {
    $location = new Bio::Location::Split();
    while (@translateable_exons) {
      my $exon = shift @translateable_exons;
      $location->add_sub_Location (
				   new Bio::Location::Simple(-start => $exon->start(),
							     -end => $exon->end(),
							     -strand => $exon->strand(),
							    )
				  );
    }
  }

#  warn "Found translation ".$transcript->external_name()."\n";

  my $feature = new Bio::SeqFeature::Generic (-primary => 'CDS',
					      -location => $location,
					      -tag => {
						       id => $translation->stable_id(),
						       name => $transcript->external_name(),
						       source => $transcript->external_db(),
						      }
					     );
  # Translations never seem to have a description
  # so we take the one from the gene instead.
  if ($gene->description()) {
    $feature -> add_tag_value(description => $gene->description());
  }

  $seq->add_SeqFeature($feature);


  my @xrefs = @{$translation->get_all_DBEntries()};

  while (@xrefs) {
    my $xref = shift @xrefs;
    next unless ($xref->dbname());
    next if ($transcript->external_db() and $xref->dbname() eq $transcript->external_db());
    next if ($xref->dbname() eq 'EMBL'); # Just too many of these!
    next if ($xref->dbname() eq 'protein_id'); # Too many of these!
#    next if ($xref->dbname() eq 'Uniprot/SPTREMBL');
    next if ($xref->dbname() eq 'IPI');

    if ($xref -> dbname() eq 'GO') {
      my $term = $GO_adapter->fetch_by_accession($xref->display_id());
      # This sometimes fails.  Just move on.
      if ($term) {
	$feature -> add_tag_value(db_xref => $xref->display_id()." ".$term->name." (".$term->namespace().")");
      }
    }

    else {
      $feature -> add_tag_value(db_xref => $xref->dbname().":".$xref->display_id());
    }

#    warn "Xref of ".$xref->display_id()." in ".$xref->dbname()."\n";
  }

}
