setwd("%%WORKING%%")

# Load Limma
library("limma")

# Read in the full count table from SeqMonk
just.raw.counts <- read.delim("counts.txt",quote="",row.names=1, check.names=FALSE)

# Make up a condition table

conditions <- as.factor(gsub("-.*","",colnames(just.raw.counts)))

design <- data.frame(All=rep(1,ncol(just.raw.counts)))

sapply(
  levels(conditions),
  function(x) {
    design[[x]]<<-rep(0,ncol(just.raw.counts))
    design[[x]][startsWith(colnames(just.raw.counts),x)] <<- 1
  }
 )

design[,-1] -> design

makeContrasts(%%CONTRASTS%% levels=design) -> contrast.matrix


# Perform the analysis.
lmFit(just.raw.counts,design) -> efit

contrasts.fit(efit,contrast.matrix) -> fit

eBayes(fit,trend=TRUE) -> fit

topTable(fit,number=nrow(just.raw.counts),adjust.method="BH") -> all.hits

# Select significant hits
all.hits[all.hits$%%CORRECTED%% <= %%PVALUE%%,] -> significant.results

# Write the hit names to a file
write.table(significant.results,file="hits.txt",row.names=TRUE,col.names=NA,quote=FALSE,sep="\t")
