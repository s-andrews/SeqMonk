/**
 * Copyright Copyright 2010-19 Simon Andrews
 *
 *    This file is part of SeqMonk.
 *
 *    SeqMonk is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    SeqMonk is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with SeqMonk; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package uk.ac.babraham.SeqMonk.Menu;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParserRunner;
import uk.ac.babraham.SeqMonk.AnnotationParsers.GFF3AnnotationParser;
import uk.ac.babraham.SeqMonk.AnnotationParsers.GenericAnnotationParser;
import uk.ac.babraham.SeqMonk.AnnotationParsers.ProbeListAnnotationParser;
import uk.ac.babraham.SeqMonk.AnnotationParsers.SeqMonkAnnotationReimportParser;
import uk.ac.babraham.SeqMonk.DataParsers.ActiveProbeListParser;
import uk.ac.babraham.SeqMonk.DataParsers.BismarkCovFileParser;
import uk.ac.babraham.SeqMonk.DataParsers.MethylKitFileParser;
import uk.ac.babraham.SeqMonk.DataParsers.QuasRFileParser;
import uk.ac.babraham.SeqMonk.DataParsers.VisibleStoresParser;
import uk.ac.babraham.SeqMonk.DataParsers.BAMFileParser;
import uk.ac.babraham.SeqMonk.DataParsers.BedFileParser;
import uk.ac.babraham.SeqMonk.DataParsers.BedPEFileParser;
import uk.ac.babraham.SeqMonk.DataParsers.BowtieFileParser;
import uk.ac.babraham.SeqMonk.DataParsers.GenericSeqReadParser;
import uk.ac.babraham.SeqMonk.DataParsers.GffFileParser;
import uk.ac.babraham.SeqMonk.DataParsers.HiCOtherEndExtractor;
import uk.ac.babraham.SeqMonk.DataParsers.SeqMonkDataReimportParser;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.DataWriters.BedGraphDataWriter;
import uk.ac.babraham.SeqMonk.Dialogs.AboutDialog;
import uk.ac.babraham.SeqMonk.Dialogs.AnnotationTrackSelector;
import uk.ac.babraham.SeqMonk.Dialogs.AutoSplitDataDialog;
import uk.ac.babraham.SeqMonk.Dialogs.DataZoomSelector;
import uk.ac.babraham.SeqMonk.Dialogs.EditPreferencesDialog;
import uk.ac.babraham.SeqMonk.Dialogs.FindFeatureDialog;
import uk.ac.babraham.SeqMonk.Dialogs.FindFeaturesByNameDialog;
import uk.ac.babraham.SeqMonk.Dialogs.GroupEditor;
import uk.ac.babraham.SeqMonk.Dialogs.LicenseDialog;
import uk.ac.babraham.SeqMonk.Dialogs.ListOverlapsDialog;
import uk.ac.babraham.SeqMonk.Dialogs.ProbeListSelectorDialog;
import uk.ac.babraham.SeqMonk.Dialogs.ReplicateSetEditor;
import uk.ac.babraham.SeqMonk.Dialogs.AnnotationSetEditor.AnnotationSetEditor;
import uk.ac.babraham.SeqMonk.Dialogs.DataSetEditor.DataSetEditor;
import uk.ac.babraham.SeqMonk.Dialogs.DataTrackSelector.DataTrackSelector;
import uk.ac.babraham.SeqMonk.Dialogs.Filters.FilterOptionsDialog;
import uk.ac.babraham.SeqMonk.Dialogs.GotoDialog.GotoDialog;
import uk.ac.babraham.SeqMonk.Dialogs.GotoDialog.GotoWindowDialog;
import uk.ac.babraham.SeqMonk.Displays.AlignedProbePlot.AlignedSummaryPreferencesDialog;
import uk.ac.babraham.SeqMonk.Displays.BeanPlot.MultiBeanPlotDialog;
import uk.ac.babraham.SeqMonk.Displays.BoxWhisker.MultiBoxWhiskerDialog;
import uk.ac.babraham.SeqMonk.Displays.SmallRNAQCPlot.SmallRNAQCPreferencesDialog;
import uk.ac.babraham.SeqMonk.Displays.StarWars.MultiStarWarsDialog;
import uk.ac.babraham.SeqMonk.Displays.StrandBias.StrandBiasPlotDialog;
import uk.ac.babraham.SeqMonk.Displays.TsneDataStorePlot.TsneOptionsDialog;
import uk.ac.babraham.SeqMonk.Displays.VariancePlot.VariancePlotDialog;
import uk.ac.babraham.SeqMonk.Displays.Vistory.VistoryDialog;
import uk.ac.babraham.SeqMonk.Displays.VolcanoPlot.VolcanoPlotDialog;
import uk.ac.babraham.SeqMonk.Displays.CisTransScatterplot.CisTransScatterPlotDialog;
import uk.ac.babraham.SeqMonk.Displays.CorrelationMatrix.CorrelationMatrix;
import uk.ac.babraham.SeqMonk.Displays.CumulativeDistribution.CumulativeDistributionDialog;
import uk.ac.babraham.SeqMonk.Displays.DataStoreTree.DataStoreTreeDialog;
import uk.ac.babraham.SeqMonk.Displays.Domainogram.DomainogramPreferencesDialog;
import uk.ac.babraham.SeqMonk.Displays.DuplicationPlot.DuplicationPlotDialog;
import uk.ac.babraham.SeqMonk.Displays.GiraphPlot.GiraphPlot;
import uk.ac.babraham.SeqMonk.Displays.Help.HelpDialog;
import uk.ac.babraham.SeqMonk.Displays.HiCHeatmap.HeatmapGenomeWindow;
import uk.ac.babraham.SeqMonk.Displays.HiCHeatmap.HeatmapProbeListWindow;
import uk.ac.babraham.SeqMonk.Displays.HierarchicalClusterPlot.HierarchicalClusterDialog;
import uk.ac.babraham.SeqMonk.Displays.HistogramPlot.HiCLengthHistogramPlot;
import uk.ac.babraham.SeqMonk.Displays.HistogramPlot.ProbeLengthHistogramPlot;
import uk.ac.babraham.SeqMonk.Displays.HistogramPlot.ProbeValueHistogramPlot;
import uk.ac.babraham.SeqMonk.Displays.HistogramPlot.ReadLengthHistogramPlot;
import uk.ac.babraham.SeqMonk.Displays.LineGraph.LineGraphDialog;
import uk.ac.babraham.SeqMonk.Displays.MAPlot.MAPlotDialog;
import uk.ac.babraham.SeqMonk.Displays.PCAPlot.PCADataCalculator;
import uk.ac.babraham.SeqMonk.Displays.ProbeListReport.ProbeListReportCreator;
import uk.ac.babraham.SeqMonk.Displays.ProbeTrendPlot.TrendOverProbePreferencesDialog;
import uk.ac.babraham.SeqMonk.Displays.QQDistributionPlot.QQDistributionDialog;
import uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.QuantitationTrendHeatmapPreferencesDialog;
import uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.QuantitationTrendPlotPreferencesDialog;
import uk.ac.babraham.SeqMonk.Displays.RNASeqQCPlot.RNAQCPreferencesDialog;
import uk.ac.babraham.SeqMonk.Displays.Report.ReportOptions;
import uk.ac.babraham.SeqMonk.Displays.ScatterPlot.ScatterPlotDialog;
import uk.ac.babraham.SeqMonk.Filters.BinomialFilterForRev;
import uk.ac.babraham.SeqMonk.Filters.BoxWhiskerFilter;
import uk.ac.babraham.SeqMonk.Filters.ChiSquareFilter;
import uk.ac.babraham.SeqMonk.Filters.ChiSquareFilterForRev;
import uk.ac.babraham.SeqMonk.Filters.ChiSquareFilterFrontBack;
import uk.ac.babraham.SeqMonk.Filters.CollateListsFilter;
import uk.ac.babraham.SeqMonk.Filters.CombineFilter;
import uk.ac.babraham.SeqMonk.Filters.CorrelationFilter;
import uk.ac.babraham.SeqMonk.Filters.DeduplicationFilter;
import uk.ac.babraham.SeqMonk.Filters.DifferencesFilter;
import uk.ac.babraham.SeqMonk.Filters.DistributionPositionFilter;
import uk.ac.babraham.SeqMonk.Filters.DuplicateListFilter;
import uk.ac.babraham.SeqMonk.Filters.FeatureFilter;
import uk.ac.babraham.SeqMonk.Filters.FeatureNameFilter;
import uk.ac.babraham.SeqMonk.Filters.IntensityDifferenceFilter;
import uk.ac.babraham.SeqMonk.Filters.IntersectListsFilter;
import uk.ac.babraham.SeqMonk.Filters.ListAnnotationValuesFilter;
import uk.ac.babraham.SeqMonk.Filters.MonteCarloFilter;
import uk.ac.babraham.SeqMonk.Filters.PositionFilter;
import uk.ac.babraham.SeqMonk.Filters.ProbeLengthFilter;
import uk.ac.babraham.SeqMonk.Filters.ProbeNameFilter;
import uk.ac.babraham.SeqMonk.Filters.ProportionOfLibraryStatisticsFilter;
import uk.ac.babraham.SeqMonk.Filters.RandomFilter;
import uk.ac.babraham.SeqMonk.Filters.ReplicateSetStatsFilter;
import uk.ac.babraham.SeqMonk.Filters.ValuesFilter;
import uk.ac.babraham.SeqMonk.Filters.WindowedDifferencesFilter;
import uk.ac.babraham.SeqMonk.Filters.WindowedReplicateStatsFilter;
import uk.ac.babraham.SeqMonk.Filters.WindowedValuesFilter;
import uk.ac.babraham.SeqMonk.Filters.CorrelationCluster.CorrelationClusterFilter;
import uk.ac.babraham.SeqMonk.Filters.DESeqFilter.DESeqFilter;
import uk.ac.babraham.SeqMonk.Filters.EdgeRFilter.EdgeRFilter;
import uk.ac.babraham.SeqMonk.Filters.EdgeRFilter.EdgeRForRevFilter;
import uk.ac.babraham.SeqMonk.Filters.GeneSetFilter.GeneSetIntensityDifferenceFilter;
import uk.ac.babraham.SeqMonk.Filters.LimmaFilter.LimmaFilter;
import uk.ac.babraham.SeqMonk.Filters.LogisticRegressionFilter.LogisticRegressionFilter;
import uk.ac.babraham.SeqMonk.Filters.LogisticRegressionFilter.LogisticRegressionSplicingFilter;
import uk.ac.babraham.SeqMonk.Filters.ManualCorrelation.ManualCorrelationFilter;
import uk.ac.babraham.SeqMonk.Filters.SegmentationFilter.SegmentationFilter;
import uk.ac.babraham.SeqMonk.Filters.Variance.VarianceIntensityDifferenceFilter;
import uk.ac.babraham.SeqMonk.Filters.Variance.VarianceValuesFilter;
import uk.ac.babraham.SeqMonk.Pipelines.Options.DefinePipelineOptions;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Preferences.Editor.DisplayPreferencesEditorDialog;
import uk.ac.babraham.SeqMonk.ProbeGenerators.DefineProbeOptions;
import uk.ac.babraham.SeqMonk.Quantitation.Options.DefineQuantitationOptions;
import uk.ac.babraham.SeqMonk.R.RVersionTest;
import uk.ac.babraham.SeqMonk.Reports.AnnotatedListReport;
import uk.ac.babraham.SeqMonk.Reports.ChromosomeViewReport;
import uk.ac.babraham.SeqMonk.Reports.DataStoreSummaryReport;
import uk.ac.babraham.SeqMonk.Reports.FeatureReport;
import uk.ac.babraham.SeqMonk.Reports.ProbeGroupReport;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * SeqMonkMenu represents the main screen menu and toolbar.
 */
public class SeqMonkMenu extends JMenuBar implements ActionListener {

	/** The main application */
	private SeqMonkApplication application;

	/*
	 * These menu items are stored so that they can be enabled selectively
	 * when a new genome is loaded or data is added.
	 * 
	 */

	private JMenu fileMenu;
	private JMenu fileImportData = null;
	private JMenu fileImportAnnotation = null;
	private JMenu viewMenu;
	private JMenu plotsMenu;
	private JMenu dataMenu;
	private JMenu filterMenu;
	private JMenu reportMenu;
	private JMenuItem fileSave = null;
	private JMenuItem fileSaveAs = null;
	private JMenuItem editFindFeature;
	private JMenuItem editFindFeatureNames;
	private JMenuItem editGotoPosition;
	private JMenuItem editGotoWindow;
	private JMenuItem editCopyPosition;
	private JMenu fileExportImage = null;

	private SeqMonkToolbar [] toolbars;
	private ToolbarPanel toolbarPanel = new ToolbarPanel();

	/** The array of key events is used to construct the file menu to add the recently used files */
	private static final int [] numericKeyEvents = new int [] {KeyEvent.VK_0,KeyEvent.VK_1,KeyEvent.VK_2,KeyEvent.VK_3,KeyEvent.VK_4,KeyEvent.VK_5,KeyEvent.VK_6,KeyEvent.VK_7,KeyEvent.VK_8,KeyEvent.VK_9};


	/**
	 * Instantiates a new seq monk menu.
	 * 
	 * @param application The main SeqMonk application to which the menu will be attached.
	 */
	public SeqMonkMenu (SeqMonkApplication application){
		this.application = application;

		toolbars = new SeqMonkToolbar [] {
				new MainSeqMonkToolbar(this),
				new HiCSeqMonkToolbar(this),
		};

		updateVisibleToolBars();

		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.addMenuListener(new MenuListener() {

			public void menuSelected(MenuEvent e) {
				buildFileMenu();
			}

			public void menuDeselected(MenuEvent e) {}

			public void menuCanceled(MenuEvent e) {}
		});
		fileMenu.setEnabled(false);

		add(fileMenu);

		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);

		editCopyPosition = new JMenuItem("Copy current position");
		editCopyPosition.setActionCommand("copy_position");
		editCopyPosition.setAccelerator(KeyStroke.getKeyStroke('C', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		editCopyPosition.setMnemonic(KeyEvent.VK_C);
		editCopyPosition.addActionListener(this);
		editCopyPosition.setEnabled(false);
		editMenu.add(editCopyPosition);

		editGotoPosition = new JMenuItem("Goto Position...");
		editGotoPosition.setActionCommand("goto_position");
		editGotoPosition.setAccelerator(KeyStroke.getKeyStroke('G', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		editGotoPosition.setMnemonic(KeyEvent.VK_G);
		editGotoPosition.addActionListener(this);
		editGotoPosition.setEnabled(false);
		editMenu.add(editGotoPosition);

		editGotoWindow = new JMenuItem("Goto Window...");
		editGotoWindow.setActionCommand("goto_window");
		editGotoWindow.setAccelerator(KeyStroke.getKeyStroke('G', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
		editGotoWindow.setMnemonic(KeyEvent.VK_W);
		editGotoWindow.addActionListener(this);
		editGotoWindow.setEnabled(false);
		editMenu.add(editGotoWindow);


		editFindFeature = new JMenuItem("Find Feature...");
		editFindFeature.setActionCommand("find_feature");
		editFindFeature.setAccelerator(KeyStroke.getKeyStroke('F', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		editFindFeature.setMnemonic(KeyEvent.VK_F);
		editFindFeature.addActionListener(this);
		editFindFeature.setEnabled(false);
		editMenu.add(editFindFeature);

		editFindFeatureNames = new JMenuItem("Find Named Features...");
		editFindFeatureNames.setActionCommand("find_feature_names");
		editFindFeatureNames.setMnemonic(KeyEvent.VK_N);
		editFindFeatureNames.addActionListener(this);
		editFindFeatureNames.setEnabled(false);
		editMenu.add(editFindFeatureNames);


		editMenu.addSeparator();

		JMenuItem editPreferences = new JMenuItem("Preferences...");
		editPreferences.setActionCommand("edit_preferences");
		editPreferences.setMnemonic(KeyEvent.VK_R);
		editPreferences.addActionListener(this);
		editMenu.add(editPreferences);

		add(editMenu);


		viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);
		viewMenu.setEnabled(false);

		JMenu viewToolbarsMenu = new JMenu("Toolbars");
		viewToolbarsMenu.setMnemonic(KeyEvent.VK_T);

		for (int i=0;i<toolbars.length;i++) {
			JCheckBoxMenuItem toolbarItem = new JCheckBoxMenuItem(toolbars[i].name(),toolbars[i].shown());
			toolbarItem.setActionCommand("toolbar_"+i);
			toolbarItem.addActionListener(this);
			viewToolbarsMenu.add(toolbarItem);
		}

		viewMenu.add(viewToolbarsMenu);
		viewMenu.addSeparator();


		JCheckBoxMenuItem viewAllLabels = new JCheckBoxMenuItem("Show All Labels");
		viewAllLabels.setSelected(false);
		viewAllLabels.setActionCommand("view_all_labels");
		viewAllLabels.setAccelerator(KeyStroke.getKeyStroke('L', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		viewAllLabels.addActionListener(this);
		viewAllLabels.setMnemonic(KeyEvent.VK_L);
		viewMenu.add(viewAllLabels);

		JMenuItem viewDataTrackDisplay = new JMenuItem("Data Track Display...");
		viewDataTrackDisplay.setActionCommand("view_display_options");
		viewDataTrackDisplay.addActionListener(this);
		viewDataTrackDisplay.setMnemonic(KeyEvent.VK_T);
		viewMenu.add(viewDataTrackDisplay);

		viewMenu.addSeparator();

		JMenuItem viewDataTracks = new JMenuItem("Set Data Tracks...");
		viewDataTracks.setActionCommand("view_data_tracks");
		viewDataTracks.setMnemonic(KeyEvent.VK_D);
		viewDataTracks.addActionListener(this);
		viewMenu.add(viewDataTracks);


		JMenuItem viewAnnotationTracks = new JMenuItem("Set Annotation Tracks...");
		viewAnnotationTracks.setActionCommand("view_annotation_tracks");
		viewAnnotationTracks.setMnemonic(KeyEvent.VK_A);
		viewAnnotationTracks.addActionListener(this);
		viewMenu.add(viewAnnotationTracks);

		viewMenu.addSeparator();


		JMenuItem viewSetZoom = new JMenuItem("Set Data Zoom Level...");
		viewSetZoom.setActionCommand("view_set_zoom");
		viewSetZoom.setAccelerator(KeyStroke.getKeyStroke('Z', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		viewSetZoom.setMnemonic(KeyEvent.VK_Z);
		viewSetZoom.addActionListener(this);
		viewMenu.add(viewSetZoom);

		JMenuItem viewZoomIn = new JMenuItem("Zoom In");
		viewZoomIn.setActionCommand("zoom_in");
		viewZoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
		viewZoomIn.setMnemonic(KeyEvent.VK_I);
		viewZoomIn.addActionListener(this);
		viewMenu.add(viewZoomIn);

		JMenuItem viewZoomOut = new JMenuItem("Zoom Out");
		viewZoomOut.setActionCommand("zoom_out");
		viewZoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
		viewZoomOut.setMnemonic(KeyEvent.VK_O);
		viewZoomOut.addActionListener(this);
		viewMenu.add(viewZoomOut);

		JMenuItem viewMoveLeft = new JMenuItem("Move left");
		viewMoveLeft.setActionCommand("move_left");
		viewMoveLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
		viewMoveLeft.setMnemonic(KeyEvent.VK_L);
		viewMoveLeft.addActionListener(this);
		viewMenu.add(viewMoveLeft);

		JMenuItem viewMoveRight = new JMenuItem("Move Right");
		viewMoveRight.setActionCommand("move_right");
		viewMoveRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
		viewMoveRight.setMnemonic(KeyEvent.VK_R);
		viewMoveRight.addActionListener(this);
		viewMenu.add(viewMoveRight);

		JMenuItem viewPageLeft = new JMenuItem("Page left");
		viewPageLeft.setActionCommand("page_left");
		viewPageLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK));
		viewPageLeft.addActionListener(this);
		viewMenu.add(viewPageLeft);

		JMenuItem viewPageRight = new JMenuItem("Page Right");
		viewPageRight.setActionCommand("page_right");
		viewPageRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK));
		viewPageRight.addActionListener(this);
		viewMenu.add(viewPageRight);

		

		add(viewMenu);

		dataMenu = new JMenu("Data");
		dataMenu.setMnemonic(KeyEvent.VK_D);
		dataMenu.setEnabled(false);

		JMenuItem dataDefineProbes = new JMenuItem("Define Probes...");
		dataDefineProbes.setActionCommand("define_probes");
		dataDefineProbes.setMnemonic(KeyEvent.VK_D);
		dataDefineProbes.addActionListener(this);
		dataMenu.add(dataDefineProbes);

		JMenuItem dataQuantitate = new JMenuItem("Quantitate Existing Probes...");
		dataQuantitate.setActionCommand("quantitation");
		dataQuantitate.setMnemonic(KeyEvent.VK_Q);
		dataQuantitate.addActionListener(this);
		dataMenu.add(dataQuantitate);

		JMenuItem dataSpecialQuantitate = new JMenuItem("Quantitation Pipelines...");
		dataSpecialQuantitate.setActionCommand("pipeline_quantitation");
		dataSpecialQuantitate.setMnemonic(KeyEvent.VK_P);
		dataSpecialQuantitate.addActionListener(this);
		dataMenu.add(dataSpecialQuantitate);

		dataMenu.addSeparator();

		JMenuItem dataSamples = new JMenuItem("Edit Data Sets...");
		dataSamples.setActionCommand("edit_samples");
		dataSamples.setMnemonic(KeyEvent.VK_S);
		dataSamples.addActionListener(this);
		dataMenu.add(dataSamples);

		JMenuItem dataGroups = new JMenuItem("Edit Groups...");
		dataGroups.setActionCommand("edit_groups");
		dataGroups.setMnemonic(KeyEvent.VK_G);
		dataGroups.addActionListener(this);
		dataMenu.add(dataGroups);

		JMenuItem dataReplicates = new JMenuItem("Edit Replicate Sets...");
		dataReplicates.setActionCommand("edit_replicates");
		dataReplicates.setMnemonic(KeyEvent.VK_R);
		dataReplicates.addActionListener(this);
		dataMenu.add(dataReplicates);

		JMenuItem dataAutoCreateGroups = new JMenuItem("Auto Create Groups/Sets...");
		dataAutoCreateGroups.setActionCommand("auto_create_groups");
		dataAutoCreateGroups.addActionListener(this);
		dataAutoCreateGroups.setMnemonic(KeyEvent.VK_A);
		dataMenu.add(dataAutoCreateGroups);

		dataMenu.addSeparator();
		
		JMenuItem dataAnnotations = new JMenuItem("Edit Annotation Sets...");
		dataAnnotations.setActionCommand("edit_annotations");
		dataAnnotations.setMnemonic(KeyEvent.VK_A);
		dataAnnotations.addActionListener(this);
		dataMenu.add(dataAnnotations);


		add(dataMenu);

		plotsMenu = new JMenu("Plots");
		plotsMenu.setMnemonic(KeyEvent.VK_P);
		plotsMenu.setEnabled(false);

		JMenuItem viewProbeValHist = new JMenuItem("Probe Value Histogram...");
		viewProbeValHist.setActionCommand("view_value_histogram");
		viewProbeValHist.setMnemonic(KeyEvent.VK_H);
		viewProbeValHist.addActionListener(this);
		plotsMenu.add(viewProbeValHist);

		JMenuItem viewReadLenHist = new JMenuItem("Read Length Histogram...");
		viewReadLenHist.setActionCommand("view_read_length_histogram");
		viewReadLenHist.setMnemonic(KeyEvent.VK_R);
		viewReadLenHist.addActionListener(this);
		plotsMenu.add(viewReadLenHist);

		JMenuItem viewProbeLenHist = new JMenuItem("Probe Length Histogram...");
		viewProbeLenHist.setActionCommand("view_probe_length_histogram");
		viewProbeLenHist.setMnemonic(KeyEvent.VK_L);
		viewProbeLenHist.addActionListener(this);
		plotsMenu.add(viewProbeLenHist);

		plotsMenu.addSeparator();

		JMenu storeSimilarityMenu = new JMenu("Data Store Similarity");

		JMenuItem viewDataStoreTree = new JMenuItem("DataStore Tree...");
		viewDataStoreTree.setActionCommand("view_datastore_tree");
		viewDataStoreTree.setMnemonic(KeyEvent.VK_T);
		viewDataStoreTree.addActionListener(this);
		storeSimilarityMenu.add(viewDataStoreTree);

		JMenuItem viewPCA = new JMenuItem("PCA Plot...");
		viewPCA.setActionCommand("view_pca");
		viewPCA.setMnemonic(KeyEvent.VK_P);
		viewPCA.addActionListener(this);
		storeSimilarityMenu.add(viewPCA);

		JMenuItem viewTSNE = new JMenuItem("TSNE Plot...");
		viewTSNE.setActionCommand("view_tsne");
		viewTSNE.setMnemonic(KeyEvent.VK_T);
		viewTSNE.addActionListener(this);
		storeSimilarityMenu.add(viewTSNE);
		
		
		JMenuItem viewCorrelationMatrix = new JMenuItem("Correlation Matrix...");
		viewCorrelationMatrix.setActionCommand("view_correlation_matrix");
		viewCorrelationMatrix.setMnemonic(KeyEvent.VK_M);
		viewCorrelationMatrix.addActionListener(this);
		storeSimilarityMenu.add(viewCorrelationMatrix);

		plotsMenu.add(storeSimilarityMenu);
		
		
		JMenuItem plotListOverlapMenu = new JMenu("Probe List Overlap");
		plotListOverlapMenu.setMnemonic(KeyEvent.VK_O);

		JMenuItem plotListOverlapMatrix = new JMenuItem("Probe List Overlap Matrix...");
		plotListOverlapMatrix.setActionCommand("multiprobe_plot_list_overlap");
		plotListOverlapMatrix.setMnemonic(KeyEvent.VK_O);
		plotListOverlapMatrix.addActionListener(this);
		plotListOverlapMenu.add(plotListOverlapMatrix);

		JMenuItem plotListOverlapPlot = new JMenuItem("Probe List Overlap Plot...");
		plotListOverlapPlot.setActionCommand("multiprobe_list_giraph_plot");
		//plotListOverlapMatrix.setMnemonic(KeyEvent.VK_G);
		plotListOverlapPlot.addActionListener(this);
		plotListOverlapMenu.add(plotListOverlapPlot);

		plotsMenu.add(plotListOverlapMenu);

		/*JMenuItem plotListOverlapMatrix = new JMenuItem("Probe List Overlap Matrix...");
		plotListOverlapMatrix.setActionCommand("multiprobe_plot_list_overlap");
		plotListOverlapMatrix.setMnemonic(KeyEvent.VK_O);
		plotListOverlapMatrix.addActionListener(this);
		plotsMenu.add(plotListOverlapMatrix);
		 */
		JMenuItem viewHierarchicalClusterMenu = new JMenu("Hierarchical Clusters");
		viewHierarchicalClusterMenu.setMnemonic(KeyEvent.VK_C);

		JMenuItem viewHierarchicalClusterPlot = new JMenuItem("Current Quantitation");
		viewHierarchicalClusterPlot.setActionCommand("view_hierarchical_cluster");
		viewHierarchicalClusterPlot.setMnemonic(KeyEvent.VK_C);
		viewHierarchicalClusterPlot.addActionListener(this);
		viewHierarchicalClusterMenu.add(viewHierarchicalClusterPlot);

		JMenuItem plotDomainogram = new JMenuItem("Domainogram...");
		plotDomainogram.setActionCommand("plot_domainogram");
		plotDomainogram.addActionListener(this);
		plotDomainogram.setMnemonic(KeyEvent.VK_D);
		plotsMenu.add(plotDomainogram);

		JMenuItem viewHierarchicalClusterPlotNormalise = new JMenuItem("Per-probe Normalised");
		viewHierarchicalClusterPlotNormalise.setActionCommand("view_hierarchical_cluster_normalised");
		viewHierarchicalClusterPlotNormalise.setMnemonic(KeyEvent.VK_N);
		viewHierarchicalClusterPlotNormalise.addActionListener(this);
		viewHierarchicalClusterMenu.add(viewHierarchicalClusterPlotNormalise);

		plotsMenu.add(viewHierarchicalClusterMenu);

		plotsMenu.addSeparator();

		JMenu viewCumDist = new JMenu("Cumulative Distribution Plot");
		viewCumDist.setMnemonic(KeyEvent.VK_C);

		JMenuItem viewCumDistStores = new JMenuItem("Visible Data Stores...");
		viewCumDistStores.setActionCommand("view_cumdist");
		viewCumDistStores.setMnemonic(KeyEvent.VK_S);
		viewCumDistStores.addActionListener(this);
		viewCumDist.add(viewCumDistStores);

		JMenuItem viewCumDistProbes = new JMenuItem("Multiple Probe Lists...");
		viewCumDistProbes.setActionCommand("multiprobe_view_cumdist");
		viewCumDistProbes.setMnemonic(KeyEvent.VK_M);
		viewCumDistProbes.addActionListener(this);
		viewCumDist.add(viewCumDistProbes);		

		plotsMenu.add(viewCumDist);

		JMenu viewQQDist = new JMenu("QQ Distribution Plot");
		viewQQDist.setMnemonic(KeyEvent.VK_Q);

		JMenuItem viewQQDistStores = new JMenuItem("Visible Data Stores...");
		viewQQDistStores.setActionCommand("view_qqdist");
		viewQQDistStores.setMnemonic(KeyEvent.VK_S);
		viewQQDistStores.addActionListener(this);
		viewQQDist.add(viewQQDistStores);

		JMenuItem viewQQDistProbes = new JMenuItem("Multiple Probe Lists...");
		viewQQDistProbes.setActionCommand("multiprobe_view_qqdist");
		viewQQDistProbes.setMnemonic(KeyEvent.VK_M);
		viewQQDistProbes.addActionListener(this);
		viewQQDist.add(viewQQDistProbes);		

		plotsMenu.add(viewQQDist);

		
		JMenu viewBeanPlot = new JMenu("Bean Plot");
		viewBeanPlot.setMnemonic(KeyEvent.VK_B);

		JMenuItem viewBeanPlotStores = new JMenuItem("Visible Data Stores...");
		viewBeanPlotStores.setActionCommand("view_beanplot");
		viewBeanPlotStores.setMnemonic(KeyEvent.VK_S);
		viewBeanPlotStores.addActionListener(this);
		viewBeanPlot.add(viewBeanPlotStores);

		JMenuItem viewBeanPlotProbes = new JMenuItem("Multiple Probe Lists...");
		viewBeanPlotProbes.setActionCommand("multiprobe_view_beanplot");
		viewBeanPlotProbes.setMnemonic(KeyEvent.VK_M);
		viewBeanPlotProbes.addActionListener(this);
		viewBeanPlot.add(viewBeanPlotProbes);		

		JMenuItem viewBeanPlotMulti = new JMenuItem("Multiple Probe Lists and DataStores...");
		viewBeanPlotMulti.setActionCommand("multiprobe_view_multistore_beanplot");
		viewBeanPlotMulti.setMnemonic(KeyEvent.VK_D);
		viewBeanPlotMulti.addActionListener(this);
		viewBeanPlot.add(viewBeanPlotMulti);		


		plotsMenu.add(viewBeanPlot);

		

		JMenu viewBoxWhisker = new JMenu("Box Whisker Plot");
		viewBoxWhisker.setMnemonic(KeyEvent.VK_X);

		JMenuItem viewBoxWhiskerStores = new JMenuItem("Visible Data Stores...");
		viewBoxWhiskerStores.setActionCommand("view_boxwhisker");
		viewBoxWhiskerStores.setMnemonic(KeyEvent.VK_S);
		viewBoxWhiskerStores.addActionListener(this);
		viewBoxWhisker.add(viewBoxWhiskerStores);

		JMenuItem viewBoxWhiskerProbes = new JMenuItem("Multiple Probe Lists...");
		viewBoxWhiskerProbes.setActionCommand("multiprobe_view_boxwhisker");
		viewBoxWhiskerProbes.setMnemonic(KeyEvent.VK_M);
		viewBoxWhiskerProbes.addActionListener(this);
		viewBoxWhisker.add(viewBoxWhiskerProbes);		

		JMenuItem viewBoxWhiskerMulti = new JMenuItem("Multiple Probe Lists and DataStores...");
		viewBoxWhiskerMulti.setActionCommand("multiprobe_view_multistore_boxwhisker");
		viewBoxWhiskerMulti.setMnemonic(KeyEvent.VK_D);
		viewBoxWhiskerMulti.addActionListener(this);
		viewBoxWhisker.add(viewBoxWhiskerMulti);		


		plotsMenu.add(viewBoxWhisker);

		JMenu viewStarWars = new JMenu("Star Wars Plot");
		viewStarWars.setMnemonic(KeyEvent.VK_B);

		JMenuItem viewStarWarsStores = new JMenuItem("Visible Data Stores...");
		viewStarWarsStores.setActionCommand("view_starwars");
		viewStarWarsStores.setMnemonic(KeyEvent.VK_S);
		viewStarWarsStores.addActionListener(this);
		viewStarWars.add(viewStarWarsStores);

		JMenuItem viewStarWarsProbes = new JMenuItem("Multiple Probe Lists...");
		viewStarWarsProbes.setActionCommand("multiprobe_view_starwars");
		viewStarWarsProbes.setMnemonic(KeyEvent.VK_M);
		viewStarWarsProbes.addActionListener(this);
		viewStarWars.add(viewStarWarsProbes);		

		JMenuItem viewStarWarsMulti = new JMenuItem("Multiple Probe Lists and DataStores...");
		viewStarWarsMulti.setActionCommand("multiprobe_view_multistore_starwars");
		viewStarWarsMulti.setMnemonic(KeyEvent.VK_D);
		viewStarWarsMulti.addActionListener(this);
		viewStarWars.add(viewStarWarsMulti);		


		plotsMenu.add(viewStarWars);

//		JMenuItem viewVennDiagram = new JMenuItem("Venn Diagram...");
//		viewVennDiagram.setActionCommand("view_venn_diagram");
//		viewVennDiagram.setMnemonic(KeyEvent.VK_V);
//		viewVennDiagram.addActionListener(this);
//		plotsMenu.add(viewVennDiagram);

		plotsMenu.addSeparator();

		JMenu viewProbeTrend = new JMenu("Probe Trend Plot...");


		JMenuItem viewProbeTrendCurrentProbes = new JMenuItem("Current Probe List...");
		viewProbeTrendCurrentProbes.setActionCommand("view_probetrend");
		viewProbeTrendCurrentProbes.setMnemonic(KeyEvent.VK_C);
		viewProbeTrendCurrentProbes.addActionListener(this);
		viewProbeTrend.add(viewProbeTrendCurrentProbes);

		JMenuItem viewProbeTrendMultiProbes = new JMenuItem("Multiple Probe Lists...");
		viewProbeTrendMultiProbes.setActionCommand("multiprobe_view_probetrend");
		viewProbeTrendMultiProbes.setMnemonic(KeyEvent.VK_M);
		viewProbeTrendMultiProbes.addActionListener(this);
		viewProbeTrend.add(viewProbeTrendMultiProbes);

		plotsMenu.add(viewProbeTrend);

		JMenu plotAlignedMenu = new JMenu("Aligned Probes Plot");
		plotAlignedMenu.setMnemonic(KeyEvent.VK_A);

		JMenuItem viewAlignedProbes = new JMenuItem("Active Probe List...");
		viewAlignedProbes.setActionCommand("view_aligned_probes");
		viewAlignedProbes.setMnemonic(KeyEvent.VK_A);
		viewAlignedProbes.addActionListener(this);
		plotAlignedMenu.add(viewAlignedProbes);

		JMenuItem viewAlignedProbesMulti = new JMenuItem("Multiple Probe Lists...");
		viewAlignedProbesMulti.setActionCommand("view_aligned_probes_multi");
		viewAlignedProbesMulti.setMnemonic(KeyEvent.VK_M);
		viewAlignedProbesMulti.addActionListener(this);
		plotAlignedMenu.add(viewAlignedProbesMulti);

		plotsMenu.add(plotAlignedMenu);

//		JMenuItem plotCodonBiasPlot = new JMenuItem("Codon Bias Plot...");
//		plotCodonBiasPlot.setActionCommand("plot_codon_bias");
//		plotCodonBiasPlot.setMnemonic(KeyEvent.VK_B);
//		plotCodonBiasPlot.addActionListener(this);
//		plotsMenu.add(plotCodonBiasPlot);
		
		JMenu viewQuantTrendPlots = new JMenu("Quantitation Trend Plots..");
		viewQuantTrendPlots.setMnemonic(KeyEvent.VK_Q);

		JMenu viewQuantitationTrend = new JMenu("Quantitation Trend Plot");
		viewQuantitationTrend.setMnemonic(KeyEvent.VK_P);
		
		JMenuItem viewQuantitationTrendCurrent = new JMenuItem("Current Probe List...");
		viewQuantitationTrendCurrent.setActionCommand("view_quanttrend");
		viewQuantitationTrendCurrent.setMnemonic(KeyEvent.VK_C);
		viewQuantitationTrendCurrent.addActionListener(this);
		viewQuantitationTrend.add(viewQuantitationTrendCurrent);
		
		JMenuItem viewQuantitationTrendMulti = new JMenuItem("Multiple Probe Lists...");
		viewQuantitationTrendMulti.setActionCommand("multiprobe_view_quanttrend");
		viewQuantitationTrendMulti.setMnemonic(KeyEvent.VK_M);
		viewQuantitationTrendMulti.addActionListener(this);
		viewQuantitationTrend.add(viewQuantitationTrendMulti);
		
		viewQuantTrendPlots.add(viewQuantitationTrend);

		
		JMenu viewQuantitationHeatmap = new JMenu("Quantitation Trend Heatmap");
		viewQuantitationHeatmap.setMnemonic(KeyEvent.VK_H);
		
		JMenuItem viewQuantitationHeatCurrent = new JMenuItem("Current Probe List...");
		viewQuantitationHeatCurrent.setActionCommand("view_quantheat");
		viewQuantitationHeatCurrent.setMnemonic(KeyEvent.VK_C);
		viewQuantitationHeatCurrent.addActionListener(this);
		viewQuantitationHeatmap.add(viewQuantitationHeatCurrent);
		
		JMenuItem viewQuantitationHeatMulti = new JMenuItem("Multiple Probe Lists...");
		viewQuantitationHeatMulti.setActionCommand("multiprobe_view_quantheat");
		viewQuantitationHeatMulti.setMnemonic(KeyEvent.VK_M);
		viewQuantitationHeatMulti.addActionListener(this);
		viewQuantitationHeatmap.add(viewQuantitationHeatMulti);
		
		viewQuantTrendPlots.add(viewQuantitationHeatmap);
		
		plotsMenu.add(viewQuantTrendPlots);

		
		plotsMenu.addSeparator();

		JMenu viewHeatMap = new JMenu("HiC Heatmap");
		viewHeatMap.setMnemonic(KeyEvent.VK_H);

		JMenuItem viewGenomeHeatMap = new JMenuItem("Genome View");
		viewGenomeHeatMap.setMnemonic(KeyEvent.VK_G);
		viewGenomeHeatMap.setActionCommand("view_heatmap_active");
		viewGenomeHeatMap.addActionListener(this);
		viewHeatMap.add(viewGenomeHeatMap);

		JMenuItem viewProbeListHeatMap = new JMenuItem("Probe List View");
		viewProbeListHeatMap.setMnemonic(KeyEvent.VK_P);
		viewProbeListHeatMap.setActionCommand("multiprobe_view_heatmap_active");
		viewProbeListHeatMap.addActionListener(this);
		viewHeatMap.add(viewProbeListHeatMap);

		plotsMenu.add(viewHeatMap);

		JMenuItem cisTransScatterPlot = new JMenuItem("HiC Cis/Trans Scatterplot");
		cisTransScatterPlot.setMnemonic(KeyEvent.VK_C);
		cisTransScatterPlot.setActionCommand("cis_trans_scatterplot");
		cisTransScatterPlot.addActionListener(this);
		plotsMenu.add(cisTransScatterPlot);

		JMenu viewHiCLenMenu = new JMenu("HiC Length Histogram");
		viewHiCLenMenu.setMnemonic(KeyEvent.VK_C);

		JMenuItem viewHiCLenHist = new JMenuItem("Whole Genome");
		viewHiCLenHist.setActionCommand("view_hic_length_histogram");
		viewHiCLenHist.setMnemonic(KeyEvent.VK_G);
		viewHiCLenHist.addActionListener(this);
		viewHiCLenMenu.add(viewHiCLenHist);

		JMenuItem viewHiCLenProbes = new JMenuItem("Active Probe List");
		viewHiCLenProbes.setActionCommand("view_hic_length_probes");
		viewHiCLenProbes.setMnemonic(KeyEvent.VK_P);
		viewHiCLenProbes.addActionListener(this);
		viewHiCLenMenu.add(viewHiCLenProbes);

		plotsMenu.add(viewHiCLenMenu);


//		JMenuItem viewDistanceValueHistogram = new JMenuItem("Distance Value Histogram");
//		viewDistanceValueHistogram.setActionCommand("view_distance_value_histogram");
//		viewDistanceValueHistogram.setMnemonic(KeyEvent.VK_V);
//		viewDistanceValueHistogram.addActionListener(this);
//		plotsMenu.add(viewDistanceValueHistogram);


		plotsMenu.addSeparator();

		JMenuItem viewScatterPlot = new JMenuItem("Scatter Plot...");
		viewScatterPlot.setActionCommand("view_scatterplot");
		viewScatterPlot.setMnemonic(KeyEvent.VK_S);
		viewScatterPlot.addActionListener(this);
		plotsMenu.add(viewScatterPlot);

		JMenuItem viewMAPlot = new JMenuItem("MA Plot...");
		viewMAPlot.setActionCommand("view_ma_plot");
		viewMAPlot.setMnemonic(KeyEvent.VK_M);
		viewMAPlot.addActionListener(this);
		plotsMenu.add(viewMAPlot);

		JMenuItem viewVolcanoPlot = new JMenuItem("Volcano Plot...");
		viewVolcanoPlot.setActionCommand("view_volcano_plot");
		viewVolcanoPlot.setMnemonic(KeyEvent.VK_V);
		viewVolcanoPlot.addActionListener(this);
		plotsMenu.add(viewVolcanoPlot);
		
		JMenuItem plotDuplication = new JMenuItem("Duplication Plot...");
		plotDuplication.setActionCommand("plot_duplication");
		plotDuplication.setMnemonic(KeyEvent.VK_D);
		plotDuplication.addActionListener(this);
		plotsMenu.add(plotDuplication);

		JMenuItem viewVariancePlot = new JMenuItem("Variation Plot...");
		viewVariancePlot.setActionCommand("view_variance_plot");
		viewVariancePlot.setMnemonic(KeyEvent.VK_P);
		viewVariancePlot.addActionListener(this);
		plotsMenu.add(viewVariancePlot);

		JMenuItem viewStrandBiasPlot = new JMenuItem("Strand Bias Plot...");
		viewStrandBiasPlot.setActionCommand("view_strand_bias_plot");
		viewStrandBiasPlot.setMnemonic(KeyEvent.VK_B);
		viewStrandBiasPlot.addActionListener(this);
		plotsMenu.add(viewStrandBiasPlot);
		
		
		JMenu viewLineGraph = new JMenu("Line Graph...");
		viewLineGraph.setMnemonic(KeyEvent.VK_L);

		JMenuItem viewLineGraphActiveList = new JMenuItem("Active Probe List...");
		viewLineGraphActiveList.setActionCommand("view_line_graph");
		viewLineGraphActiveList.setMnemonic(KeyEvent.VK_A);
		viewLineGraphActiveList.addActionListener(this);
		viewLineGraph.add(viewLineGraphActiveList);

		JMenuItem viewLineGraphMultiple = new JMenuItem("Multiple Probe Lists...");
		viewLineGraphMultiple.setActionCommand("multiprobe_view_line_graph");
		viewLineGraphMultiple.setMnemonic(KeyEvent.VK_M);
		viewLineGraphMultiple.addActionListener(this);
		viewLineGraph.add(viewLineGraphMultiple);		

		plotsMenu.add(viewLineGraph);

		plotsMenu.addSeparator();

		JMenuItem plotRNAQC = new JMenuItem("RNA-Seq QC Plot...");
		plotRNAQC.setActionCommand("plot_rna_qc");
		plotRNAQC.addActionListener(this);
		plotsMenu.add(plotRNAQC);

		JMenuItem plotSmallRNAQC = new JMenuItem("Small RNA QC Plot...");
		plotSmallRNAQC.setActionCommand("plot_small_rna_qc");
		plotSmallRNAQC.addActionListener(this);
		plotsMenu.add(plotSmallRNAQC);

		add(plotsMenu);

		filterMenu = new JMenu("Filtering");
		filterMenu.setMnemonic(KeyEvent.VK_T);
		filterMenu.setEnabled(false);

		JMenu filterValuesMenu = new JMenu("Filter on Values");
		filterValuesMenu.setMnemonic(KeyEvent.VK_V);

		JMenuItem filterValues = new JMenuItem("Individual probes...");
		filterValues.setActionCommand("filter_values_individual");
		filterValues.addActionListener(this);
		filterValues.setMnemonic(KeyEvent.VK_I);
		filterValuesMenu.add(filterValues);


		JMenuItem filterValuesW = new JMenuItem("Windowed average...");
		filterValuesW.setActionCommand("filter_values_windowed");
		filterValuesW.addActionListener(this);
		filterValuesW.setMnemonic(KeyEvent.VK_W);
		filterValuesMenu.add(filterValuesW);

		JMenuItem filterValuesP = new JMenuItem("Position in distribution...");
		filterValuesP.setActionCommand("filter_values_distribution");
		filterValuesP.addActionListener(this);
		filterValuesP.setMnemonic(KeyEvent.VK_P);
		filterValuesMenu.add(filterValuesP);

		JMenuItem filterListAnotValue = new JMenuItem("List annotation value...");
		filterListAnotValue.setActionCommand("filter_values_list_annotation");
		filterListAnotValue.addActionListener(this);
		filterListAnotValue.setMnemonic(KeyEvent.VK_A);
		filterValuesMenu.add(filterListAnotValue);


		filterMenu.add(filterValuesMenu);

		JMenu filterDiffsMenu = new JMenu("Filter on Value Differences");
		filterDiffsMenu.setMnemonic(KeyEvent.VK_D);

		JMenuItem filterDiffs = new JMenuItem("Individual probes...");
		filterDiffs.setActionCommand("filter_diffs_individual");
		filterDiffs.addActionListener(this);
		filterDiffs.setMnemonic(KeyEvent.VK_I);
		filterDiffsMenu.add(filterDiffs);


		JMenuItem filterDiffsW = new JMenuItem("Windowed average...");
		filterDiffsW.setActionCommand("filter_diffs_windowed");
		filterDiffsW.addActionListener(this);
		filterDiffsW.setMnemonic(KeyEvent.VK_W);
		filterDiffsMenu.add(filterDiffsW);

		filterMenu.add(filterDiffsMenu);

		JMenu filterVarianceMenu = new JMenu("Filter on Variance");
		filterVarianceMenu.setMnemonic(KeyEvent.VK_V);

		JMenuItem filterVariance = new JMenuItem("Individual Replicate Sets...");
		filterVariance.setActionCommand("filter_var_individual");
		filterVariance.addActionListener(this);
		filterVariance.setMnemonic(KeyEvent.VK_I);
		filterVarianceMenu.add(filterVariance);

		filterMenu.add(filterVarianceMenu);

		JMenu filterStatsMenu = new JMenu("Filter by Statistical Test");
		filterStatsMenu.setMnemonic(KeyEvent.VK_S);

		// Continuous Statistics
		JMenu continuousStatsMenu = new JMenu("Continuous value statistics");
		continuousStatsMenu.setMnemonic(KeyEvent.VK_C);
		filterStatsMenu.add(continuousStatsMenu);
		
		JMenu continuousStatsReplicatedMenu = new JMenu("Replicated data");
		continuousStatsReplicatedMenu.setMnemonic(KeyEvent.VK_R);
		continuousStatsMenu.add(continuousStatsReplicatedMenu);
		
		JMenu continuousStatsUnreplicatedMenu = new JMenu("Unreplicated data");
		continuousStatsUnreplicatedMenu.setMnemonic(KeyEvent.VK_U);
		continuousStatsMenu.add(continuousStatsUnreplicatedMenu);


		// Count Based Statistics
		JMenu countStatsMenu = new JMenu("Count based statistics");
		countStatsMenu.setMnemonic(KeyEvent.VK_O);
		filterStatsMenu.add(countStatsMenu);
		
		JMenu countStatsReplicatedMenu = new JMenu("Replicated data");
		countStatsReplicatedMenu.setMnemonic(KeyEvent.VK_R);
		countStatsMenu.add(countStatsReplicatedMenu);
		
		JMenu countStatsUnreplicatedMenu = new JMenu("Unreplicated data");
		countStatsUnreplicatedMenu.setMnemonic(KeyEvent.VK_U);
		countStatsMenu.add(countStatsUnreplicatedMenu);

		
		// Proportion Based Statistics
		JMenu proportionStatsMenu = new JMenu("Proportion based statistics");
		proportionStatsMenu.setMnemonic(KeyEvent.VK_P);
		filterStatsMenu.add(proportionStatsMenu);
		
		JMenu proportionStatsReplicatedMenu = new JMenu("Replicated data");
		proportionStatsReplicatedMenu.setMnemonic(KeyEvent.VK_R);
		proportionStatsMenu.add(proportionStatsReplicatedMenu);
		
		JMenu proportionStatsUnreplicatedMenu = new JMenu("Unreplicated data");
		proportionStatsUnreplicatedMenu.setMnemonic(KeyEvent.VK_U);
		proportionStatsMenu.add(proportionStatsUnreplicatedMenu);
		

		// Variance Statistics
		JMenu varianceStatsMenu = new JMenu("Variance statistics");
		varianceStatsMenu.setMnemonic(KeyEvent.VK_V);
		filterStatsMenu.add(varianceStatsMenu);

		// Subgroup Statistics
		JMenu subgroupStatsMenu = new JMenu("Subgroup statistics");
		subgroupStatsMenu.setMnemonic(KeyEvent.VK_S);
		filterStatsMenu.add(subgroupStatsMenu);

		// Outlier Statistics
		JMenu outlierStatsMenu = new JMenu("Outlier statistics");
		outlierStatsMenu.setMnemonic(KeyEvent.VK_O);
		filterStatsMenu.add(outlierStatsMenu);

		
		JMenuItem deseqFilter = new JMenuItem("[R] DESeq2...");
		deseqFilter.setActionCommand("filter_r_deseq2");
		deseqFilter.addActionListener(this);
		deseqFilter.setMnemonic(KeyEvent.VK_D);
		countStatsReplicatedMenu.add(deseqFilter);

		JMenuItem edgeRFilter = new JMenuItem("[R] EdgeR...");
		edgeRFilter.setActionCommand("filter_r_edger");
		edgeRFilter.addActionListener(this);
		edgeRFilter.setMnemonic(KeyEvent.VK_E);
		countStatsReplicatedMenu.add(edgeRFilter);		

		JMenuItem logisticRegressionFilter = new JMenuItem("[R] Logistic Regression (for/rev)...");
		logisticRegressionFilter.setActionCommand("filter_r_logistic_regression");
		logisticRegressionFilter.addActionListener(this);
		logisticRegressionFilter.setMnemonic(KeyEvent.VK_L);
		proportionStatsReplicatedMenu.add(logisticRegressionFilter);
		
		JMenuItem edgeRForRevFilter = new JMenuItem("[R] EdgeR (for/rev)...");
		edgeRForRevFilter.setActionCommand("filter_r_edger_forrev");
		edgeRForRevFilter.addActionListener(this);
		edgeRForRevFilter.setMnemonic(KeyEvent.VK_E);
		proportionStatsReplicatedMenu.add(edgeRForRevFilter);		


		JMenuItem logisticRegressionSplicingFilter = new JMenuItem("[R] Logistic Regression Splicing...");
		logisticRegressionSplicingFilter.setActionCommand("filter_r_logistic_regression_splicing");
		logisticRegressionSplicingFilter.addActionListener(this);
		logisticRegressionSplicingFilter.setMnemonic(KeyEvent.VK_S);
		proportionStatsReplicatedMenu.add(logisticRegressionSplicingFilter);

		
		JMenuItem filterIntensityStats = new JMenuItem("Intensity Difference...");
		filterIntensityStats.setActionCommand("filter_intensity_stats");
		filterIntensityStats.addActionListener(this);
		filterIntensityStats.setMnemonic(KeyEvent.VK_I);
		continuousStatsUnreplicatedMenu.add(filterIntensityStats);


		JMenuItem filterIntensityVarStats = new JMenuItem("Variance intensity difference...");
		filterIntensityVarStats.setActionCommand("filter_intensity_var_stats");
		filterIntensityVarStats.addActionListener(this);
		filterIntensityVarStats.setMnemonic(KeyEvent.VK_V);
		varianceStatsMenu.add(filterIntensityVarStats);


		JMenuItem filterGeneSets = new JMenuItem("Gene Set Enrichment...");
		filterGeneSets.setActionCommand("filter_genesets");
		filterGeneSets.addActionListener(this);
		subgroupStatsMenu.add(filterGeneSets);


		
		JMenuItem proportionOfLibraryStats = new JMenuItem("Proportion of library...");
		proportionOfLibraryStats.setActionCommand("filter_proportion_library");
		proportionOfLibraryStats.addActionListener(this);
		proportionOfLibraryStats.setMnemonic(KeyEvent.VK_P);
		countStatsUnreplicatedMenu.add(proportionOfLibraryStats);


		JMenuItem windowedReplicate = new JMenuItem("Windowed Replicate...");
		windowedReplicate.setActionCommand("filter_statsr");
		windowedReplicate.addActionListener(this);
		windowedReplicate.setMnemonic(KeyEvent.VK_R);
		continuousStatsUnreplicatedMenu.add(windowedReplicate);

		JMenuItem repSetStats = new JMenuItem("Replicate Set Stats (t-test/ANOVA)...");
		repSetStats.setActionCommand("filter_stats");
		repSetStats.addActionListener(this);
		repSetStats.setMnemonic(KeyEvent.VK_S);
		continuousStatsReplicatedMenu.add(repSetStats);

		JMenuItem limmaStats = new JMenuItem("[R] LIMMA Stats...");
		limmaStats.setActionCommand("filter_r_limma");
		limmaStats.addActionListener(this);
		limmaStats.setMnemonic(KeyEvent.VK_L);
		continuousStatsReplicatedMenu.add(limmaStats);
		
		JMenuItem filterMonteCarlo = new JMenuItem("Monte Carlo Stats...");
		filterMonteCarlo.setActionCommand("filter_monte_carlo");
		filterMonteCarlo.addActionListener(this);
		filterMonteCarlo.setMnemonic(KeyEvent.VK_M);
		subgroupStatsMenu.add(filterMonteCarlo);

		JMenuItem filterBoxWhisker = new JMenuItem("BoxWhisker Outlier Detection...");
		filterBoxWhisker.setActionCommand("filter_boxwhisker");
		filterBoxWhisker.addActionListener(this);
		filterBoxWhisker.setMnemonic(KeyEvent.VK_B);
		outlierStatsMenu.add(filterBoxWhisker);


		JMenuItem filterChiSquareForRev = new JMenuItem("Chi-Square (for/rev)...");
		filterChiSquareForRev.setActionCommand("filter_chisquare_forrev");
		filterChiSquareForRev.addActionListener(this);
		filterChiSquareForRev.setMnemonic(KeyEvent.VK_F);
		proportionStatsUnreplicatedMenu.add(filterChiSquareForRev);

		JMenuItem filterChiSquareStores = new JMenuItem("Chi-Square (multiple stores)...");
		filterChiSquareStores.setActionCommand("filter_chisquare_stores");
		filterChiSquareStores.addActionListener(this);
		filterChiSquareStores.setMnemonic(KeyEvent.VK_S);
		proportionStatsUnreplicatedMenu.add(filterChiSquareStores);

		JMenuItem filterChiSquareFrontBack = new JMenuItem("Chi-Square (Front/Back)...");
		filterChiSquareFrontBack.setActionCommand("filter_chisquare_frontback");
		filterChiSquareFrontBack.addActionListener(this);
		filterChiSquareFrontBack.setMnemonic(KeyEvent.VK_B);
		proportionStatsUnreplicatedMenu.add(filterChiSquareFrontBack);

		

		JMenuItem filterBinomialForRev = new JMenuItem("Binomial (for/rev)...");
		filterBinomialForRev.setActionCommand("filter_binomial_forrev");
		filterBinomialForRev.addActionListener(this);
		filterBinomialForRev.setMnemonic(KeyEvent.VK_F);
		proportionStatsUnreplicatedMenu.add(filterBinomialForRev);


		filterMenu.add(filterStatsMenu);

		JMenu filterCorrelationMenu = new JMenu("Filter by Correlation");
		filterCorrelationMenu.setMnemonic(KeyEvent.VK_C);

		JMenuItem filterCorrelationCluster = new JMenuItem("Filter by Correlation Cluster...");
		filterCorrelationCluster.setActionCommand("filter_correlation_cluster");
		filterCorrelationCluster.addActionListener(this);
		filterCorrelationCluster.setMnemonic(KeyEvent.VK_C);
		filterCorrelationMenu.add(filterCorrelationCluster);

		JMenuItem filterCorrelationProbeList = new JMenuItem("Filter by Probe List Correlation...");
		filterCorrelationProbeList.setActionCommand("filter_correlation_probes");
		filterCorrelationProbeList.addActionListener(this);
		filterCorrelationProbeList.setMnemonic(KeyEvent.VK_P);
		filterCorrelationMenu.add(filterCorrelationProbeList);

		JMenuItem filterCorrelationManual = new JMenuItem("Filter by Manual Correlation...");
		filterCorrelationManual.setActionCommand("filter_correlation_manual");
		filterCorrelationManual.addActionListener(this);
		filterCorrelationManual.setMnemonic(KeyEvent.VK_M);
		filterCorrelationMenu.add(filterCorrelationManual);


		filterMenu.add(filterCorrelationMenu);
		
		JMenuItem filterSegmentation = new JMenuItem("Filter by Segmentation");
		filterSegmentation.setActionCommand("filter_segmentation");
		filterSegmentation.addActionListener(this);
		filterSegmentation.setMnemonic(KeyEvent.VK_S);
		filterMenu.add(filterSegmentation);
		

		JMenuItem filterPosition = new JMenuItem("Filter by Position...");
		filterPosition.setActionCommand("filter_position");
		filterPosition.addActionListener(this);
		filterPosition.setMnemonic(KeyEvent.VK_P);
		filterMenu.add(filterPosition);

		JMenuItem filterDeduplication = new JMenuItem("Deduplication Filter...");
		filterDeduplication.setActionCommand("filter_deduplication");
		filterDeduplication.addActionListener(this);
		filterDeduplication.setMnemonic(KeyEvent.VK_D);
		filterMenu.add(filterDeduplication);

		JMenuItem filterLength = new JMenuItem("Filter by Probe Length...");
		filterLength.setActionCommand("filter_length");
		filterLength.addActionListener(this);
		filterLength.setMnemonic(KeyEvent.VK_L);
		filterMenu.add(filterLength);

//		JMenuItem filterSpacing = new JMenuItem("Filter by Inter-Probe Distance...");
//		filterSpacing.setActionCommand("filter_spacing");
//		filterSpacing.addActionListener(this);
//		filterSpacing.setMnemonic(KeyEvent.VK_D);
//		filterMenu.add(filterSpacing);

		JMenuItem filterFeatures = new JMenuItem("Filter by Features...");
		filterFeatures.setActionCommand("filter_features");
		filterFeatures.addActionListener(this);
		filterFeatures.setMnemonic(KeyEvent.VK_F);
		filterMenu.add(filterFeatures);

		JMenuItem filterFeatureNames = new JMenuItem("Filter by Feature names...");
		filterFeatureNames.setActionCommand("filter_feature_names");
		filterFeatureNames.addActionListener(this);
		filterFeatureNames.setMnemonic(KeyEvent.VK_N);
		filterMenu.add(filterFeatureNames);

		JMenuItem filterProbeNames = new JMenuItem("Filter by Probe names...");
		filterProbeNames.setActionCommand("filter_probe_names");
		filterProbeNames.addActionListener(this);
		filterProbeNames.setMnemonic(KeyEvent.VK_P);
		filterMenu.add(filterProbeNames);

		JMenuItem filterRandom = new JMenuItem("Filter Random Probes...");
		filterRandom.setActionCommand("filter_random");
		filterRandom.addActionListener(this);
		filterRandom.setMnemonic(KeyEvent.VK_R);
		filterMenu.add(filterRandom);
		
		JMenuItem filterDuplicateList = new JMenuItem("Duplicate Existing List");
		filterDuplicateList.setActionCommand("filter_duplicate_list");
		filterDuplicateList.addActionListener(this);
		filterDuplicateList.setMnemonic(KeyEvent.VK_D);
		filterMenu.add(filterDuplicateList);
		

		JMenu combineListsMenu = new JMenu("Combine existing lists");


		JMenuItem filterCombine = new JMenuItem("Logically Combine Two Lists...");
		filterCombine.setActionCommand("filter_combine");
		filterCombine.addActionListener(this);
		filterCombine.setMnemonic(KeyEvent.VK_C);
		combineListsMenu.add(filterCombine);

		JMenuItem filterIntersect = new JMenuItem("Intersect Multiple Lists...");
		filterIntersect.setActionCommand("filter_intersect");
		filterIntersect.addActionListener(this);
		filterIntersect.setMnemonic(KeyEvent.VK_I);
		combineListsMenu.add(filterIntersect);
		
		JMenuItem filterCollate = new JMenuItem("Collate Multiple Lists...");
		filterCollate.setActionCommand("filter_collate");
		filterCollate.addActionListener(this);
		filterCollate.setMnemonic(KeyEvent.VK_L);
		combineListsMenu.add(filterCollate);


		

		filterMenu.add(combineListsMenu);
		
		
		add(filterMenu);

		reportMenu = new JMenu("Reports");
		reportMenu.setMnemonic(KeyEvent.VK_R);
		reportMenu.setEnabled(false);

		JMenuItem reportAnnotated = new JMenuItem("Annotated Probe Report...");
		reportAnnotated.setActionCommand("report_annotated");
		reportAnnotated.addActionListener(this);
		reportAnnotated.setMnemonic(KeyEvent.VK_A);
		reportMenu.add(reportAnnotated);

		JMenuItem reportProbeGroup = new JMenuItem("Probe Group Report...");
		reportProbeGroup.setActionCommand("report_group");
		reportProbeGroup.addActionListener(this);
		reportProbeGroup.setMnemonic(KeyEvent.VK_G);
		reportMenu.add(reportProbeGroup);

		JMenuItem reportFeatures = new JMenuItem("Feature Report...");
		reportFeatures.setActionCommand("report_feature");
		reportFeatures.addActionListener(this);
		reportFeatures.setMnemonic(KeyEvent.VK_F);
		reportMenu.add(reportFeatures);

		JMenuItem reportChromosomeView = new JMenuItem("Chromosome View Report...");
		reportChromosomeView.setActionCommand("report_chromosome");
		reportChromosomeView.addActionListener(this);
		reportChromosomeView.setMnemonic(KeyEvent.VK_C);
		reportMenu.add(reportChromosomeView);


		JMenuItem reportSummary = new JMenuItem("DataStore Summary Report...");
		reportSummary.setActionCommand("report_summary");
		reportSummary.addActionListener(this);
		reportSummary.setMnemonic(KeyEvent.VK_S);
		reportMenu.add(reportSummary);

		reportMenu.addSeparator();

		JMenuItem reportProbeListDescription = new JMenuItem("Save Probe List Description Report...");
		reportProbeListDescription.setActionCommand("report_description");
		reportProbeListDescription.addActionListener(this);
		reportProbeListDescription.setMnemonic(KeyEvent.VK_D);
		reportMenu.add(reportProbeListDescription);

		reportMenu.addSeparator();

		JMenuItem reportShowVistory = new JMenuItem("Show Vistory...");
		reportShowVistory.setActionCommand("report_vistory");
		reportShowVistory.addActionListener(this);
		reportShowVistory.setMnemonic(KeyEvent.VK_V);
		reportShowVistory.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));

		reportMenu.add(reportShowVistory);

		add(reportMenu);



		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_H);

		JMenuItem helpContents = new JMenuItem("Contents...");
		helpContents.setActionCommand("help_contents");
		helpContents.setAccelerator(KeyStroke.getKeyStroke('H', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		helpContents.addActionListener(this);
		helpContents.setMnemonic(KeyEvent.VK_C);
		helpMenu.add(helpContents);

		helpMenu.addSeparator();

		JMenuItem helpLicense = new JMenuItem("License...");
		helpLicense.setActionCommand("help_license");
		helpLicense.setMnemonic(KeyEvent.VK_L);
		helpLicense.addActionListener(this);
		helpMenu.add(helpLicense);


		JMenuItem helpAbout = new JMenuItem("About...");
		helpAbout.setActionCommand("about");
		helpAbout.setMnemonic(KeyEvent.VK_A);
		helpAbout.addActionListener(this);
		helpMenu.add(helpAbout);

		add(helpMenu);

	}

	/**
	 * Builds the file menu.
	 */
	private void buildFileMenu () {

		fileMenu.removeAll();

		JMenuItem fileNew = new JMenuItem("New project...");
		fileNew.setActionCommand("new");
		fileNew.setAccelerator(KeyStroke.getKeyStroke('N', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileNew.setMnemonic(KeyEvent.VK_N);
		fileNew.addActionListener(this);
		fileMenu.add(fileNew);

		JMenuItem fileOpen = new JMenuItem ("Open project...");
		fileOpen.setActionCommand("open");
		fileOpen.setAccelerator(KeyStroke.getKeyStroke('O', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileOpen.addActionListener(this);
		fileOpen.setMnemonic(KeyEvent.VK_O);
		fileMenu.add(fileOpen);		

		JMenuItem fileOpenSwitch = new JMenuItem ("Open project and switch assembly...");
		fileOpenSwitch.setActionCommand("open_switch");
		fileOpenSwitch.addActionListener(this);
		fileOpenSwitch.setMnemonic(KeyEvent.VK_W);
		fileMenu.add(fileOpenSwitch);		

		
		fileMenu.addSeparator();

		if (fileImportData == null) {
			fileImportData = new JMenu("Import Data");
			fileImportData.setMnemonic(KeyEvent.VK_I);
			fileImportData.setEnabled(false);

			JMenuItem fileImportGeneric = new JMenuItem("Text (Generic)...");
			fileImportGeneric.setMnemonic(KeyEvent.VK_T);
			fileImportGeneric.setActionCommand("import_generic");
			fileImportGeneric.addActionListener(this);
			fileImportData.add(fileImportGeneric);

			JMenuItem fileImportBam = new JMenuItem("BAM/SAM...");
			fileImportBam.setAccelerator(KeyStroke.getKeyStroke('I', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileImportBam.setMnemonic(KeyEvent.VK_B);
			fileImportBam.setActionCommand("import_bam");
			fileImportBam.addActionListener(this);
			fileImportData.add(fileImportBam);

			JMenuItem fileImportBed = new JMenuItem("BED...");
			fileImportBed.setMnemonic(KeyEvent.VK_B);
			fileImportBed.setActionCommand("import_bed");
			fileImportBed.addActionListener(this);
			fileImportData.add(fileImportBed);

			JMenuItem fileImportBedPE = new JMenuItem("BEDPE...");
			fileImportBedPE.setMnemonic(KeyEvent.VK_P);
			fileImportBedPE.setActionCommand("import_bedpe");
			fileImportBedPE.addActionListener(this);
			fileImportData.add(fileImportBedPE);
			
			JMenuItem fileImportBismarkCov = new JMenuItem("Bismark (Cov)...");
			fileImportBismarkCov.setMnemonic(KeyEvent.VK_C);
			fileImportBismarkCov.setActionCommand("import_bismark_cov");
			fileImportBismarkCov.addActionListener(this);
			fileImportData.add(fileImportBismarkCov);

			JMenuItem fileImportQuasR = new JMenuItem("QuasR...");
			fileImportQuasR.setMnemonic(KeyEvent.VK_Q);
			fileImportQuasR.setActionCommand("import_quasr");
			fileImportQuasR.addActionListener(this);
			fileImportData.add(fileImportQuasR);

			JMenuItem fileImportMethylKit = new JMenuItem("MethylKit...");
			fileImportMethylKit.setMnemonic(KeyEvent.VK_M);
			fileImportMethylKit.setActionCommand("import_methylkit");
			fileImportMethylKit.addActionListener(this);
			fileImportData.add(fileImportMethylKit);

			JMenuItem fileImportBowtie = new JMenuItem("Bowtie...");
			fileImportBowtie.setMnemonic(KeyEvent.VK_W);
			fileImportBowtie.setActionCommand("import_bowtie");
			fileImportBowtie.addActionListener(this);
			fileImportData.add(fileImportBowtie);

			JMenuItem fileImportGff = new JMenuItem("GFF...");
			fileImportGff.setMnemonic(KeyEvent.VK_G);
			fileImportGff.setActionCommand("import_gff");
			fileImportGff.addActionListener(this);
			fileImportData.add(fileImportGff);

			JMenuItem fileImportSeqMonk = new JMenuItem("SeqMonk Project...");
			fileImportSeqMonk.setMnemonic(KeyEvent.VK_S);
			fileImportSeqMonk.setActionCommand("import_seqmonk");
			fileImportSeqMonk.addActionListener(this);
			fileImportData.add(fileImportSeqMonk);

			JMenuItem fileImportVisibleStore = new JMenuItem("Visible Data Stores...");
			fileImportVisibleStore.setMnemonic(KeyEvent.VK_A);
			fileImportVisibleStore.setActionCommand("import_visible_stores");
			fileImportVisibleStore.addActionListener(this);
			fileImportData.add(fileImportVisibleStore);

			JMenuItem fileImportActiveProbeList = new JMenuItem("Active Probe List...");
			fileImportActiveProbeList.setMnemonic(KeyEvent.VK_P);
			fileImportActiveProbeList.setActionCommand("import_active_probes");
			fileImportActiveProbeList.addActionListener(this);
			fileImportData.add(fileImportActiveProbeList);


			JMenuItem fileImportHiC = new JMenuItem("HiC Other Ends...");
			fileImportHiC.setMnemonic(KeyEvent.VK_H);
			fileImportHiC.setActionCommand("import_hic");
			fileImportHiC.addActionListener(this);
			fileImportData.add(fileImportHiC);

			JMenuItem fileImportOther = new JMenuItem("Other format...");
			fileImportOther.setMnemonic(KeyEvent.VK_O);
			fileImportOther.setActionCommand("import_other");
			fileImportOther.addActionListener(this);
			fileImportData.add(fileImportOther);

		}



		fileMenu.add(fileImportData);

		if (fileImportAnnotation == null) {
			fileImportAnnotation = new JMenu("Import Annotation");
			fileImportAnnotation.setMnemonic(KeyEvent.VK_N);
			fileImportAnnotation.setEnabled(false);

			JMenuItem fileImportGeneric = new JMenuItem("Text (Generic)...");
			fileImportGeneric.setMnemonic(KeyEvent.VK_G);
			fileImportGeneric.setActionCommand("annot_generic");
			fileImportGeneric.addActionListener(this);
			fileImportAnnotation.add(fileImportGeneric);

			JMenuItem fileImportGff = new JMenuItem("GFF/GTF...");
			fileImportGff.setMnemonic(KeyEvent.VK_G);
			fileImportGff.setActionCommand("annot_gff");
			fileImportGff.addActionListener(this);
			fileImportAnnotation.add(fileImportGff);
			
			JMenuItem fileImportSeqmonkAnnotation = new JMenuItem("Existing SeqMonk Project...");
			fileImportSeqmonkAnnotation.setMnemonic(KeyEvent.VK_S);
			fileImportSeqmonkAnnotation.setActionCommand("annot_seqmonk");
			fileImportSeqmonkAnnotation.addActionListener(this);
			fileImportAnnotation.add(fileImportSeqmonkAnnotation);

			JMenuItem fileImportProbeList = new JMenuItem("Active Probe List");
			fileImportProbeList.setMnemonic(KeyEvent.VK_P);
			fileImportProbeList.setActionCommand("annot_probe");
			fileImportProbeList.addActionListener(this);
			fileImportAnnotation.add(fileImportProbeList);

		}

		fileMenu.add(fileImportAnnotation);

		fileMenu.addSeparator();

		if (fileExportImage == null) {

			fileExportImage = new JMenu("Export Current View");
			fileExportImage.setMnemonic(KeyEvent.VK_E);
			fileExportImage.setEnabled(false);

			JMenuItem fileExportImageChromosome = new JMenuItem("Chromosome View");
			fileExportImageChromosome.setMnemonic(KeyEvent.VK_C);
			fileExportImageChromosome.setActionCommand("file_export_chromosome_view");
			fileExportImageChromosome.addActionListener(this);
			fileExportImage.add(fileExportImageChromosome);


			JMenuItem fileExportImageGenome = new JMenuItem("Genome View");
			fileExportImageGenome.setMnemonic(KeyEvent.VK_G);
			fileExportImageGenome.setActionCommand("file_export_genome_view");
			fileExportImageGenome.addActionListener(this);
			fileExportImage.add(fileExportImageGenome);

			JMenuItem fileExportBedGraph = new JMenuItem("BEDGraph");
			fileExportBedGraph.setMnemonic(KeyEvent.VK_B);
			fileExportBedGraph.setActionCommand("file_export_bedgraph");
			fileExportBedGraph.addActionListener(this);
			fileExportImage.add(fileExportBedGraph);

		}
		fileMenu.add(fileExportImage);


		fileMenu.addSeparator();

		if (fileSave == null) {
			fileSave = new JMenuItem("Save Project");
			fileSave.setActionCommand("save");
			fileSave.addActionListener(this);
			fileSave.setAccelerator(KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileSave.setMnemonic(KeyEvent.VK_S);
			fileSave.setEnabled(false);
		}
		fileMenu.add(fileSave);

		if (fileSaveAs == null) {
			fileSaveAs = new JMenuItem("Save Project As...");
			fileSaveAs.setActionCommand("save_as");
			fileSaveAs.addActionListener(this);
			fileSaveAs.setMnemonic(KeyEvent.VK_A);
			fileSaveAs.setEnabled(false);
		}
		fileMenu.add(fileSaveAs);

		fileMenu.addSeparator();

		String [] recentPaths = SeqMonkPreferences.getInstance().getRecentlyOpenedFiles();
		int count = 1;
		for (int i=0;i<recentPaths.length;i++) {
			File f = new File(recentPaths[i]);
			if (f.exists()) {
				JMenuItem menuItem = new JMenuItem(count+" "+f.getName());
				menuItem.addActionListener(new FileOpener(application,f));
				menuItem.setMnemonic(numericKeyEvents[count]);
				fileMenu.add(menuItem);
				count++;
			}
		}

		fileMenu.addSeparator();

		JMenuItem fileExit = new JMenuItem("Exit");
		fileExit.setActionCommand("exit");
		fileExit.setMnemonic(KeyEvent.VK_X);
		fileExit.addActionListener(this);

		fileMenu.add(fileExit);


	}

	public JPanel toolbarPanel () {
		return toolbarPanel;
	}

	private void updateVisibleToolBars () {
		Vector<JToolBar> visibleToolBars = new Vector<JToolBar>();

		for (int i=0;i<toolbars.length;i++) {
			if (toolbars[i].shown()) {
				visibleToolBars.add(toolbars[i]);
			}
		}

		toolbarPanel.setToolBars(visibleToolBars.toArray(new JToolBar[0]));
	}

	/**
	 * Resets the menu availability to its default state.
	 * Should be called when a new dataset is loaded.
	 */
	public void resetMenus () {
		// Since load project can be called from the toolbar it's possible that
		// the file menu has never been built, so we should explicity build it
		// to be sure.
		buildFileMenu();


		fileImportData.setEnabled(false);
		fileImportAnnotation.setEnabled(false);
		viewMenu.setEnabled(false);
		plotsMenu.setEnabled(false);
		fileSave.setEnabled(false);
		fileSaveAs.setEnabled(false);
		editFindFeature.setEnabled(false);
		editFindFeatureNames.setEnabled(false);
		editGotoPosition.setEnabled(false);
		editGotoWindow.setEnabled(false);
		editCopyPosition.setEnabled(false);
		fileExportImage.setEnabled(false);
		dataMenu.setEnabled(false);
		filterMenu.setEnabled(false);
		reportMenu.setEnabled(false);
		for (int i=0;i<toolbars.length;i++) {
			toolbars[i].reset();
		}
	}

	/**
	 * Genome loaded.
	 */
	public void genomeLoaded() {
		fileImportData.setEnabled(true);
		fileImportAnnotation.setEnabled(true);
		viewMenu.setEnabled(true);
		fileSave.setEnabled(true);
		fileSaveAs.setEnabled(true);
		editFindFeature.setEnabled(true);
		editFindFeatureNames.setEnabled(true);
		editGotoPosition.setEnabled(true);
		editGotoWindow.setEnabled(true);
		editCopyPosition.setEnabled(true);
		fileExportImage.setEnabled(true);
		for (int i=0;i<toolbars.length;i++) {
			toolbars[i].genomeLoaded();
			toolbars[i].setDataCollection(application.dataCollection());
		}
	}

	/**
	 * Data loaded.
	 */
	public void dataLoaded() {
		genomeLoaded();
		dataMenu.setEnabled(true);
		plotsMenu.setEnabled(true);
		filterMenu.setEnabled(true);
		reportMenu.setEnabled(true);
	}

	public void cacheFolderChecked () {
		fileMenu.setEnabled(true);
	}


	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();

		if (action.equals("exit")) {
			application.dispose();
		}
		else if (action.equals("new")) {
			application.startNewProject();
		}
		else if (action.equals("import_other")) {
			JOptionPane.showMessageDialog(application, "SeqMonk can be extended to support other file formats.\n\nIf you'd like support for the format you use then please contact the program authors\nand provide them with example data files so they can write the appropriate import filters.", "Import other formats", JOptionPane.INFORMATION_MESSAGE);
		}
		else if (action.equals("import_generic")) {
			application.importData(new GenericSeqReadParser(application.dataCollection()));
		}
		else if (action.equals("import_bed")) {
			application.importData(new BedFileParser(application.dataCollection()));
		}
		else if (action.equals("import_bedpe")) {
			application.importData(new BedPEFileParser(application.dataCollection()));
		}
		else if (action.equals("import_bowtie")) {
			application.importData(new BowtieFileParser(application.dataCollection()));
		}
		else if (action.equals("import_bismark_cov")) {
			application.importData(new BismarkCovFileParser(application.dataCollection()));
		}
		else if (action.equals("import_quasr")) {
			application.importData(new QuasRFileParser(application.dataCollection()));
		}
		else if (action.equals("import_methylkit")) {
			application.importData(new MethylKitFileParser(application.dataCollection()));
		}
		else if (action.equals("import_gff")) {
			application.importData(new GffFileParser(application.dataCollection()));
		}
		else if (action.equals("import_seqmonk")) {
			application.importData(new SeqMonkDataReimportParser(application.dataCollection()));
		}
		else if (action.equals("import_bam")) {
			application.importData(new BAMFileParser(application.dataCollection()));
		}
		else if (action.equals("import_visible_stores")) {
			if (application.drawnDataStores() == null || application.drawnDataStores().length == 0) {
				JOptionPane.showMessageDialog(application, "You need to have some data stores visible before you can use this option", "Can't reimport data", JOptionPane.ERROR_MESSAGE);
				return;
			}
			application.importData(new VisibleStoresParser(application));
		}
		else if (action.equals("import_active_probes")) {
			if (application.dataCollection().probeSet() == null) {
				JOptionPane.showMessageDialog(application, "You don't have any active probes to import at the moment", "Can't import data", JOptionPane.ERROR_MESSAGE);
				return;
			}
			application.importData(new ActiveProbeListParser(application));
		}
		else if (action.equals("import_hic")) {
			DataStore [] drawnStores = application.drawnDataStores();
			boolean foundHic = false;
			for (int i=0;i<drawnStores.length;i++) {
				if (drawnStores[i] instanceof HiCDataStore && ((HiCDataStore)drawnStores[i]).isValidHiC()) {
					foundHic = true;
				}
			}

			if (!foundHic) {
				JOptionPane.showMessageDialog(application, "You need to have at least one visible HiC data set to use this option", "Can't import HiC data", JOptionPane.ERROR_MESSAGE);
				return;				
			}

			application.importData(new HiCOtherEndExtractor(application.dataCollection(),application.drawnDataStores()));
		}
		else if (action.equals("annot_gff")) {
			AnnotationParserRunner.RunAnnotationParser(application, new GFF3AnnotationParser(application.dataCollection().genome()));
		}
		else if (action.equals("annot_seqmonk")) {
			AnnotationParserRunner.RunAnnotationParser(application, new SeqMonkAnnotationReimportParser(application.dataCollection().genome()));
		}
		else if (action.equals("annot_generic")) {
			AnnotationParserRunner.RunAnnotationParser(application, new GenericAnnotationParser(application.dataCollection().genome()));
		}
		else if (action.equals("annot_probe")) {

			if (application.dataCollection().probeSet() == null) {
				JOptionPane.showMessageDialog(application, "You haven't made a probeset yet (Do Data > Quantitation > Define Probes)", "Can't make new annotation", JOptionPane.ERROR_MESSAGE);
				return;
			}

			String featureType=null;
			while (true) {
				featureType = (String)JOptionPane.showInputDialog(application,"Enter new feature type","Set feature type",JOptionPane.QUESTION_MESSAGE,null,null,application.dataCollection().probeSet().getActiveList().name());
				if (featureType == null)
					return;  // They cancelled

				if (featureType.length() == 0)
					continue; // Try again

				break;
			}			

			AnnotationParserRunner.RunAnnotationParser(application, new ProbeListAnnotationParser(application.dataCollection().genome(),application.dataCollection().probeSet().getActiveList(),featureType));
		}

		else if (action.equals("about")) {
			new AboutDialog();
		}
		else if (action.equals("help_license")) {
			try {
				new LicenseDialog(application);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if (action.equals("help_contents")) {
			try {

				// Java has a bug in it which affects the creation of valid URIs from
				// URLs relating to an windows UNC path.  We therefore have to mung
				// URLs starting file file:// to add 5 forward slashes so that we
				// end up with a valid URI.

				URL url = ClassLoader.getSystemResource("Help");
				if (url.toString().startsWith("file://")) {
					try {
						url = new URL(url.toString().replace("file://", "file://///"));
					} catch (MalformedURLException e) {
						throw new IllegalStateException(e);
					}
				}
				new HelpDialog(new File(url.toURI()));
			}
			catch (URISyntaxException ux) {
				System.err.println("Couldn't parse URL falling back to path");
				new HelpDialog(new File(ClassLoader.getSystemResource("Help").getPath()));				
			}
		}
		else if (action.equals("zoom_in")) {
			application.chromosomeViewer().zoomIn();
		}
		else if (action.equals("zoom_out")) {
			application.chromosomeViewer().zoomOut();
		}
		else if (action.equals("move_left")) {
			application.chromosomeViewer().moveLeft();
		}
		else if (action.equals("move_right")) {
			application.chromosomeViewer().moveRight();
		}
		else if (action.equals("page_left")) {
			application.chromosomeViewer().pageLeft();
		}
		else if (action.equals("page_right")) {
			application.chromosomeViewer().pageRight();
		}
		else if (action.equals("view_display_options")) {
			new DisplayPreferencesEditorDialog();
		}
		else if (action.equals("data_reads_probes")) {
			DisplayPreferences.getInstance().setDisplayMode(DisplayPreferences.DISPLAY_MODE_READS_AND_QUANTITATION);
		}
		else if (action.equals("data_reads")) {
			DisplayPreferences.getInstance().setDisplayMode(DisplayPreferences.DISPLAY_MODE_READS_ONLY);
		}
		else if (action.equals("data_probes")) {
			DisplayPreferences.getInstance().setDisplayMode(DisplayPreferences.DISPLAY_MODE_QUANTITATION_ONLY);
		}
		else if (action.equals("read_density_low")) {
			DisplayPreferences.getInstance().setReadDensity(DisplayPreferences.READ_DENSITY_LOW);
		}
		else if (action.equals("read_density_medium")) {
			DisplayPreferences.getInstance().setReadDensity(DisplayPreferences.READ_DENSITY_MEDIUM);
		}
		else if (action.equals("read_density_high")) {
			DisplayPreferences.getInstance().setReadDensity(DisplayPreferences.READ_DENSITY_HIGH);
		}
		else if (action.equals("read_pack_combine")) {
			DisplayPreferences.getInstance().setReadDisplay(DisplayPreferences.READ_DISPLAY_COMBINED);
		}
		else if (action.equals("read_pack_separate")) {
			DisplayPreferences.getInstance().setReadDisplay(DisplayPreferences.READ_DISPLAY_SEPARATED);
		}
		else if (action.equals("data_colour_dynamic")) {
			DisplayPreferences.getInstance().setColourType(DisplayPreferences.COLOUR_TYPE_GRADIENT);
		}
		else if (action.equals("data_colour_fixed")) {
			DisplayPreferences.getInstance().setColourType(DisplayPreferences.COLOUR_TYPE_INDEXED);
		}
		else if (action.equals("scale_positive")) {
			DisplayPreferences.getInstance().setScaleType(DisplayPreferences.SCALE_TYPE_POSITIVE);
		}
		else if (action.equals("scale_negative")) {
			DisplayPreferences.getInstance().setScaleType(DisplayPreferences.SCALE_TYPE_POSITIVE_AND_NEGATIVE);
		}
		else if (action.equals("view_annotation_tracks")) {
			new AnnotationTrackSelector(application);
		}
		else if (action.equals("view_data_tracks")) {
			new DataTrackSelector(application);
		}
		else if (action.equals("view_boxwhisker")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new MultiBoxWhiskerDialog(application.drawnDataSets(),new ProbeList [] {application.dataCollection().probeSet().getActiveList()});
			}
		}
		else if (action.equals("view_starwars")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new MultiStarWarsDialog(application.drawnDataSets(),new ProbeList [] {application.dataCollection().probeSet().getActiveList()});
			}
		}
		else if (action.equals("view_beanplot")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new MultiBeanPlotDialog(application.drawnDataSets(),new ProbeList [] {application.dataCollection().probeSet().getActiveList()});
			}
		}

//		else if (action.equals("view_venn_diagram")) {
//			if (! application.dataCollection().isQuantitated()) {
//				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
//			}
//			else {
//				new VennDiagramDialog(application.dataCollection());
//			}
//		}

		else if (action.equals("view_cumdist")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				try {
					new CumulativeDistributionDialog(application.drawnDataSets(),application.dataCollection().probeSet().getActiveList());
				} 
				catch (SeqMonkException e) {
					throw new IllegalStateException(e);
				}
			}
		}

		else if (action.equals("view_qqdist")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				try {
					new QQDistributionDialog(application.drawnDataSets(),application.dataCollection().probeSet().getActiveList());
				} 
				catch (SeqMonkException e) {
					throw new IllegalStateException(e);
				}
			}
		}

		
		else if (action.equals("plot_rna_qc")) {
			new RNAQCPreferencesDialog(application.dataCollection(), application.drawnDataSets());
		}

		else if (action.equals("plot_small_rna_qc")) {
			new SmallRNAQCPreferencesDialog(application.dataCollection());
		}

		else if (action.startsWith("toolbar_")) {

			// We're toggling the visibility of one of the toolbars;
			int index = Integer.parseInt(action.replaceAll("toolbar_", ""));

			toolbars[index].setShown(! toolbars[index].shown());
			updateVisibleToolBars();
		}

		else if (action.startsWith("multiprobe")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			ProbeList [] probeLists = ProbeListSelectorDialog.selectProbeLists();
			if (probeLists == null || probeLists.length == 0) return;

			else if (action.equals("multiprobe_view_quanttrend")) {
				new QuantitationTrendPlotPreferencesDialog(application.dataCollection(),probeLists,application.drawnDataSets());
			}
			else if (action.equals("multiprobe_view_quantheat")) {
				new QuantitationTrendHeatmapPreferencesDialog(application.dataCollection(),probeLists,application.drawnDataSets());
			}

			else if (action.equals("multiprobe_view_boxwhisker")) {
				if (application.dataCollection().getActiveDataStore() == null) {
					JOptionPane.showMessageDialog(application, "You need to select a data store from the data panel to use this view","No active data store",JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				new MultiBoxWhiskerDialog(new DataStore [] {application.dataCollection().getActiveDataStore()},probeLists);
			}
			else if (action.equals("multiprobe_view_beanplot")) {
				if (application.dataCollection().getActiveDataStore() == null) {
					JOptionPane.showMessageDialog(application, "You need to select a data store from the data panel to use this view","No active data store",JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				new MultiBeanPlotDialog(new DataStore [] {application.dataCollection().getActiveDataStore()},probeLists);
			}

			else if (action.equals("multiprobe_view_starwars")) {
				if (application.dataCollection().getActiveDataStore() == null) {
					JOptionPane.showMessageDialog(application, "You need to select a data store from the data panel to use this view","No active data store",JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				new MultiStarWarsDialog(new DataStore [] {application.dataCollection().getActiveDataStore()},probeLists);
			}
			else if (action.equals("multiprobe_view_multistore_boxwhisker")) {
				new MultiBoxWhiskerDialog(application.drawnDataSets(),probeLists);
			}
			else if (action.equals("multiprobe_view_multistore_beanplot")) {
				new MultiBeanPlotDialog(application.drawnDataSets(),probeLists);
			}
			else if (action.equals("multiprobe_view_multistore_starwars")) {
				new MultiStarWarsDialog(application.drawnDataSets(),probeLists);
			}

			else if (action.equals("multiprobe_view_probetrend")) {
				if (! application.dataCollection().isQuantitated()) {
					JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
				}
				else {
					new TrendOverProbePreferencesDialog(probeLists,application.drawnDataSets());
				}
			}

			else if (action.equals("multiprobe_view_cumdist")) {
				if (application.dataCollection().getActiveDataStore() == null) {
					JOptionPane.showMessageDialog(application, "You need to select a data store from the data panel to use this view","No active data store",JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				try {
					new CumulativeDistributionDialog(application.dataCollection().getActiveDataStore(),probeLists);
				} 
				catch (SeqMonkException e) {
					throw new IllegalStateException(e);
				}
			}
			else if (action.equals("multiprobe_view_qqdist")) {
				if (application.dataCollection().getActiveDataStore() == null) {
					JOptionPane.showMessageDialog(application, "You need to select a data store from the data panel to use this view","No active data store",JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				try {
					new QQDistributionDialog(application.dataCollection().getActiveDataStore(),probeLists);
				} 
				catch (SeqMonkException e) {
					throw new IllegalStateException(e);
				}
			}

			else if (action.equals("multiprobe_view_line_graph")){
				try {
					new LineGraphDialog(application.drawnDataSets(), probeLists);
				}
				catch (SeqMonkException sme) {
					JOptionPane.showMessageDialog(application, sme.getLocalizedMessage(),"Can't draw graph",JOptionPane.ERROR_MESSAGE);					
				}
			}
			else if (action.equals("multiprobe_view_heatmap_active")) {
				if (application.dataCollection().getActiveDataStore() == null) {
					JOptionPane.showMessageDialog(application, "You need to select an active data store to view this plot","No data selected...",JOptionPane.INFORMATION_MESSAGE);				
				}
				else if (! (application.dataCollection().getActiveDataStore() instanceof HiCDataStore && ((HiCDataStore)application.dataCollection().getActiveDataStore()).isValidHiC())) {
					JOptionPane.showMessageDialog(application, "Your active data store needs to be a HiC dataset to view this plot","Wrong data selected...",JOptionPane.INFORMATION_MESSAGE);				
				}
				else {
					new HeatmapProbeListWindow((HiCDataStore)application.dataCollection().getActiveDataStore(), probeLists, application.dataCollection().genome());
				}
			}

			else if (action.equals("multiprobe_plot_list_overlap")) {
				new ListOverlapsDialog(probeLists);
			}
			else if (action.equals("multiprobe_list_giraph_plot")) {
				
				if(probeLists.length > 50){
					JOptionPane.showMessageDialog(application, "Please select fewer than 50 probe lists to display","Too many probe lists...",JOptionPane.INFORMATION_MESSAGE);
				}
				
				else{
					new GiraphPlot(probeLists);
				}												
			}
		}
		else if (action.equals("view_value_histogram")) {
			if (application.dataCollection().getActiveDataStore() == null) {
				JOptionPane.showMessageDialog(application, "You need to select a data store in the Data panel before viewing this plot","No data selected...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				if (! application.dataCollection().getActiveDataStore().isQuantitated()) {
					JOptionPane.showMessageDialog(application, "Your active data store hasn't been quantitated yet","No quantitation...",JOptionPane.INFORMATION_MESSAGE);					
				}
				else {
					try {
						new ProbeValueHistogramPlot(application.dataCollection().getActiveDataStore(),application.dataCollection().probeSet().getActiveList());
					} 
					catch (SeqMonkException e) {
						throw new IllegalStateException(e);
					}
				}
			}
		}
		else if (action.equals("plot_duplication")) {
			if (application.dataCollection().probeSet() == null) {
				JOptionPane.showMessageDialog(application, "You need to make some probes before running this plot","No probes...",JOptionPane.INFORMATION_MESSAGE);					
			}
			else {
				new DuplicationPlotDialog(application.drawnDataSets(),application.dataCollection().probeSet().getActiveList());
			}
		}

//		else if (action.equals("view_distance_value_histogram")) {
//			if (application.dataCollection().getActiveDataStore() == null) {
//				JOptionPane.showMessageDialog(application, "You need to select a data store in the Data panel before viewing this plot","No data selected...",JOptionPane.INFORMATION_MESSAGE);
//			}
//			else {
//				if (! application.dataCollection().isQuantitated()) {
//					JOptionPane.showMessageDialog(application, "Your data hasn't been quantitated yet","No quantitation...",JOptionPane.INFORMATION_MESSAGE);					
//				}
//				else {
//					new DistanceValueDialog(application.dataCollection());
//				}
//			}
//		}

		else if (action.equals("view_read_length_histogram")) {
			if (application.dataCollection().getActiveDataStore() == null) {
				JOptionPane.showMessageDialog(application, "You need to select a data store in the Data panel before viewing this plot","No data selected...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new ReadLengthHistogramPlot(application.dataCollection().getActiveDataStore());
			}
		}

		else if (action.startsWith("view_hic_length_")) {
			if (application.dataCollection().getActiveDataStore() == null) {
				JOptionPane.showMessageDialog(application, "You need to select a data store in the Data panel before viewing this plot","No data selected...",JOptionPane.INFORMATION_MESSAGE);
			}
			else if (! (application.dataCollection().getActiveDataStore() instanceof HiCDataStore && ((HiCDataStore)application.dataCollection().getActiveDataStore()).isValidHiC())) {
				JOptionPane.showMessageDialog(application, "This plot can only be used on HiC data stores","Not HiC data...",JOptionPane.INFORMATION_MESSAGE);
			}

			else {
				if (action.equals("view_hic_length_histogram")) {
					new HiCLengthHistogramPlot((HiCDataStore)application.dataCollection().getActiveDataStore(),null);
				}
				else if (action.equals("view_hic_length_probes")) {
					if (application.dataCollection().probeSet() != null) {
						new HiCLengthHistogramPlot((HiCDataStore)application.dataCollection().getActiveDataStore(),application.dataCollection().probeSet().getActiveList());						
					}
					else {
						JOptionPane.showMessageDialog(application, "You need to have quantitated your data before selecting this option","No data selected...",JOptionPane.INFORMATION_MESSAGE);						
					}
				}
			}
		}

		else if (action.equals("cis_trans_scatterplot")) {
			// Check to see if we have any HiC datasets
			DataStore [] stores = application.dataCollection().getAllDataStores();
			boolean foundHiC = false;
			for (int s=0;s<stores.length;s++) {
				if (stores[s] instanceof HiCDataStore && ((HiCDataStore)stores[s]).isValidHiC()) {
					foundHiC = true;
					break;
				}
			}

			if (! foundHiC) {
				JOptionPane.showMessageDialog(application, "You don't appear to have any HiC data to plot","No data available...",JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			new CisTransScatterPlotDialog(application.dataCollection());
		}

		else if (action.equals("view_probe_length_histogram")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new ProbeLengthHistogramPlot(application.dataCollection().probeSet().getActiveList());
			}
		}

		else if (action.equals("view_probetrend")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new TrendOverProbePreferencesDialog(new ProbeList [] {application.dataCollection().probeSet().getActiveList()},application.drawnDataSets());
			}
		}

		else if (action.equals("view_quanttrend")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new QuantitationTrendPlotPreferencesDialog(application.dataCollection(),new ProbeList[]{application.dataCollection().probeSet().getActiveList()},application.drawnDataSets());
			}
		}

		
		else if (action.equals("view_quantheat")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new QuantitationTrendHeatmapPreferencesDialog(application.dataCollection(),new ProbeList[]{application.dataCollection().probeSet().getActiveList()},application.drawnDataSets());
			}
		}

		
//		else if (action.equals("plot_codon_bias")) {
//			new CodonBiasDialog(application.drawnDataSets(), application.dataCollection().genome().annotationCollection());
//		}
		else if (action.equals("view_aligned_probes")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new AlignedSummaryPreferencesDialog(new ProbeList[]{application.dataCollection().probeSet().getActiveList()},application.drawnDataSets());
			}
		}
		else if (action.equals("view_aligned_probes_multi")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				ProbeList [] probeLists = ProbeListSelectorDialog.selectProbeLists();
				if (probeLists == null || probeLists.length == 0) return;

				new AlignedSummaryPreferencesDialog(probeLists,application.drawnDataSets());
			}
		}

		else if (action.equals("view_heatmap_active")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else if (application.dataCollection().getActiveDataStore() == null) {
				JOptionPane.showMessageDialog(application, "You need to select an active data store to view this plot","No data selected...",JOptionPane.INFORMATION_MESSAGE);				
			}
			else if (! (application.dataCollection().getActiveDataStore() instanceof HiCDataStore && ((HiCDataStore)application.dataCollection().getActiveDataStore()).isValidHiC())) {
				JOptionPane.showMessageDialog(application, "Your active data store needs to be a HiC dataset to view this plot","Wrong data selected...",JOptionPane.INFORMATION_MESSAGE);				
			}
			else {
				new HeatmapGenomeWindow((HiCDataStore)application.dataCollection().getActiveDataStore(), application.dataCollection().probeSet().getActiveList(), application.dataCollection().genome());
			}
		}

		else if (action.equals("plot_domainogram")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new DomainogramPreferencesDialog(application.drawnDataSets(), application.dataCollection().probeSet().getActiveList());
			}
		}

		else if (action.equals("view_variance_plot")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else if (application.dataCollection().getAllReplicateSets().length == 0) {
				JOptionPane.showMessageDialog(application, "This plot only works on Replicate Sets and you don't have any","No replicate sets...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new VariancePlotDialog(application.dataCollection());
			}
		}


		else if (action.equals("view_strand_bias_plot")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new StrandBiasPlotDialog(application.dataCollection());
			}
		}

		
		
		else if (action.equals("view_scatterplot")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new ScatterPlotDialog(application.dataCollection());
			}
		}
		else if (action.equals("view_ma_plot")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new MAPlotDialog(application.dataCollection());
			}
		}
		
		else if (action.equals("view_volcano_plot")) {
			if (! (application.dataCollection().probeSet().getActiveList().getValueNames().length>=2)) {
				JOptionPane.showMessageDialog(application, "You need to select a statistical result list to draw this plot","Not a stats result...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new VolcanoPlotDialog(application.dataCollection());
			}
		}

		else if (action.equals("view_line_graph")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				try {
					new LineGraphDialog(application.drawnDataSets(), application.dataCollection().probeSet().getActiveList());
				}
				catch (SeqMonkException sme) {
					JOptionPane.showMessageDialog(application, sme.getLocalizedMessage(),"Can't draw graph",JOptionPane.ERROR_MESSAGE);					
				}
			}
		}
		else if (action.equals("view_datastore_tree")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new DataStoreTreeDialog(application.dataCollection().probeSet().getActiveList(),application.drawnDataSets());
			}
		}
		else if (action.equals("view_pca")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			
			else {
				new PCADataCalculator(application.dataCollection().probeSet().getActiveList(),application.drawnDataSets());
			}
		}

		else if (action.equals("view_tsne")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			
			else {
				new TsneOptionsDialog(application.dataCollection().probeSet().getActiveList(),application.drawnDataSets());
			}
		}

		else if (action.equals("view_correlation_matrix")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				new CorrelationMatrix(application.drawnDataSets(),application.dataCollection().probeSet().getActiveList());
			}
		}
		else if (action.startsWith("view_hierarchical_cluster")) {
			if (! application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data to view this plot","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				boolean normalise = false;
				if (action.endsWith("normalised")) {
					normalise = true;
				}
				new HierarchicalClusterDialog(application.dataCollection().probeSet().getActiveList(),application.drawnDataSets(),normalise);
			}
		}


		else if (action.equals("view_all_labels")) {
			application.chromosomeViewer().toggleLabels();
		}
		else if (action.equals("view_set_zoom")) {
			new DataZoomSelector(application);
		}
		else if (action.equals("define_probes")) {
			new DefineProbeOptions(application);
		}
		else if (action.equals("quantitation")) {
			if (application.dataCollection().probeSet() == null) {
				JOptionPane.showMessageDialog(application, "You need to define some probes before quantitating","No probes...",JOptionPane.INFORMATION_MESSAGE);
				new DefineProbeOptions(application);
			}
			else {
				new DefineQuantitationOptions(application);
			}
		}
		else if (action.equals("pipeline_quantitation")) {
			new DefinePipelineOptions(application);
		}
		else if (action.equals("edit_groups")) {
			new GroupEditor(application);
		}
		else if (action.equals("edit_replicates")) {
			new ReplicateSetEditor(application);
		}
		else if (action.equals("auto_create_groups")) {
			new AutoSplitDataDialog(application);
		}
		else if (action.equals("edit_samples")) {
			new DataSetEditor(application.dataCollection());
		}
		else if (action.equals("edit_annotations")) {
			new AnnotationSetEditor(application.dataCollection());
		}
		else if (action.equals("save")) {
			application.saveProject();
		}
		else if (action.equals("save_as")) {
			application.saveProjectAs();
		}
		else if (action.equals("file_export_chromosome_view")) {
			// We don't want the scroll bars to appear in the drawn image
			// so we temporarily remove them.
			//			application.chromosomeViewer().scrollPane().setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			//			application.chromosomeViewer().scrollPane().validate();

			ImageSaver.saveImage(application.chromosomeViewer());

			//			application.chromosomeViewer().scrollPane().setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			//			application.chromosomeViewer().scrollPane().validate();

		}
		else if (action.equals("file_export_genome_view")) {
			ImageSaver.saveImage(application.genomeViewer());
		}
		else if (action.equals("file_export_bedgraph")) {

			if (!application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You need to have quantitated your data","No quantitation...",JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			BedGraphDataWriter bgw = new BedGraphDataWriter(application.drawnDataSets(), application.dataCollection().probeSet().getActiveList());
			bgw.startProcessing();
		}
		else if (action.equals("open")) {
			application.loadProject();
		}
		else if (action.equals("open_switch")) {
			application.loadProjectAndSwitchAssembly();
		}

		else if (action.startsWith("filter")) {
			try {

				if (action.startsWith("filter_r_")) {

					try {
						RVersionTest.testRVersion(SeqMonkPreferences.getInstance().RLocation());
					}
					catch (IOException ioe) {
						JOptionPane.showMessageDialog(application, "<html>Can't find R at '"+SeqMonkPreferences.getInstance().RLocation()+"'<br><br>Please set the path to R under Edit &gt; Preferences &gt; Programs", "Can't find R", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (action.equals("filter_r_deseq2")) {
						new FilterOptionsDialog(application.dataCollection(),new DESeqFilter(application.dataCollection()));
					}
					else if (action.equals("filter_r_limma")) {
						new FilterOptionsDialog(application.dataCollection(),new LimmaFilter(application.dataCollection()));
					}
					else if (action.equals("filter_r_edger")) {
						new FilterOptionsDialog(application.dataCollection(),new EdgeRFilter(application.dataCollection()));
					}
					else if (action.equals("filter_r_edger_forrev")) {
						new FilterOptionsDialog(application.dataCollection(),new EdgeRForRevFilter(application.dataCollection()));
					}

					else if (action.equals("filter_r_logistic_regression")) {
						new FilterOptionsDialog(application.dataCollection(),new LogisticRegressionFilter(application.dataCollection()));
					}
					else if (action.equals("filter_r_logistic_regression_splicing")) {
						new FilterOptionsDialog(application.dataCollection(),new LogisticRegressionSplicingFilter(application.dataCollection()));
					}

					else {
						throw new IllegalArgumentException("Didn't understand command '"+action+"'");
					}
				}

				else if (action.equals("filter_values_individual")) {
					new FilterOptionsDialog(application.dataCollection(),new ValuesFilter(application.dataCollection()));
				}
				else if (action.equals("filter_values_windowed")) {
					new FilterOptionsDialog(application.dataCollection(),new WindowedValuesFilter(application.dataCollection()));
				}
				else if (action.equals("filter_values_distribution")) {
					new FilterOptionsDialog(application.dataCollection(),new DistributionPositionFilter(application.dataCollection()));
				}
				else if (action.equals("filter_values_list_annotation")) {
					new FilterOptionsDialog(application.dataCollection(),new ListAnnotationValuesFilter(application.dataCollection()));
				}
				else if (action.equals("filter_diffs_individual")) {
					new FilterOptionsDialog(application.dataCollection(), new DifferencesFilter(application.dataCollection()));
				}
				else if (action.equals("filter_diffs_windowed")) {
					new FilterOptionsDialog(application.dataCollection(),new WindowedDifferencesFilter(application.dataCollection()));
				}
				else if (action.equals("filter_var_individual")) {
					new FilterOptionsDialog(application.dataCollection(),new VarianceValuesFilter(application.dataCollection()));
				}
				else if (action.equals("filter_intensity_stats")) {
					new FilterOptionsDialog(application.dataCollection(), new IntensityDifferenceFilter(application.dataCollection()));
				}
				else if (action.equals("filter_intensity_var_stats")) {
					new FilterOptionsDialog(application.dataCollection(), new VarianceIntensityDifferenceFilter(application.dataCollection()));
				}
				else if (action.equals("filter_proportion_library")) {
					new FilterOptionsDialog(application.dataCollection(), new ProportionOfLibraryStatisticsFilter(application.dataCollection()));
				}
				else if (action.equals("filter_statsr")) {
					new FilterOptionsDialog(application.dataCollection(), new WindowedReplicateStatsFilter(application.dataCollection()));
				}
				else if (action.equals("filter_stats")) {
					new FilterOptionsDialog(application.dataCollection(), new ReplicateSetStatsFilter(application.dataCollection()));
				}
				else if (action.equals("filter_monte_carlo")) {
					new FilterOptionsDialog(application.dataCollection(), new MonteCarloFilter(application.dataCollection()));
				}
				// Laura 
				else if (action.equals("filter_genesets")){
					new FilterOptionsDialog(application.dataCollection(), new GeneSetIntensityDifferenceFilter(application.dataCollection()));
				}
				else if (action.equals("filter_boxwhisker")) {
					BoxWhiskerFilter filter = new BoxWhiskerFilter(application.dataCollection());
					new FilterOptionsDialog(application.dataCollection(),filter);
				}
				else if (action.equals("filter_chisquare_stores")) {
					ChiSquareFilter filter = new ChiSquareFilter(application.dataCollection());
					new FilterOptionsDialog(application.dataCollection(),filter);
				}
				else if (action.equals("filter_chisquare_forrev")) {
					ChiSquareFilterForRev filter = new ChiSquareFilterForRev(application.dataCollection());
					new FilterOptionsDialog(application.dataCollection(),filter);
				}
				else if (action.equals("filter_chisquare_frontback")) {
					ChiSquareFilterFrontBack filter = new ChiSquareFilterFrontBack(application.dataCollection());
					new FilterOptionsDialog(application.dataCollection(),filter);
				}
				else if (action.equals("filter_binomial_forrev")) {
					BinomialFilterForRev filter = new BinomialFilterForRev(application.dataCollection());
					new FilterOptionsDialog(application.dataCollection(),filter);
				}
				else if (action.equals("filter_correlation_cluster")) {
					CorrelationClusterFilter filter = new CorrelationClusterFilter(application.dataCollection());
					new FilterOptionsDialog(application.dataCollection(),filter);
				}
				else if (action.equals("filter_correlation_probes")) {
					CorrelationFilter filter = new CorrelationFilter(application.dataCollection());
					new FilterOptionsDialog(application.dataCollection(),filter);
				}
				else if (action.equals("filter_correlation_manual")) {
					ManualCorrelationFilter filter = new ManualCorrelationFilter(application.dataCollection());
					new FilterOptionsDialog(application.dataCollection(),filter);
				}
				else if (action.equals("filter_segmentation")) {
					SegmentationFilter filter = new SegmentationFilter(application.dataCollection());
					new FilterOptionsDialog(application.dataCollection(),filter);
				}

				else if (action.equals("filter_position")) {
					new FilterOptionsDialog(application.dataCollection(),new PositionFilter(application.dataCollection(),DisplayPreferences.getInstance().getCurrentChromosome(),SequenceRead.start(DisplayPreferences.getInstance().getCurrentLocation()),SequenceRead.end(DisplayPreferences.getInstance().getCurrentLocation())));
				}
//				else if (action.equals("filter_spacing")) {
//					new FilterOptionsDialog(application.dataCollection(),new InterprobeDistanceFilter(application.dataCollection()));
//				}
				else if (action.equals("filter_length")) {
					new FilterOptionsDialog(application.dataCollection(),new ProbeLengthFilter(application.dataCollection()));
				}
				else if (action.equals("filter_features")) {
					new FilterOptionsDialog(application.dataCollection(),new FeatureFilter(application.dataCollection()));

				}
				else if (action.equals("filter_feature_names")) {
					new FilterOptionsDialog(application.dataCollection(),new FeatureNameFilter(application.dataCollection()));
				}
				else if (action.equals("filter_probe_names")) {
					new FilterOptionsDialog(application.dataCollection(),new ProbeNameFilter(application.dataCollection()));
				}

				else if (action.equals("filter_deduplication")) {
					new FilterOptionsDialog(application.dataCollection(),new DeduplicationFilter(application.dataCollection()));
				}
				else if (action.equals("filter_random")) {
					new FilterOptionsDialog(application.dataCollection(),new RandomFilter(application.dataCollection()));
				}
				else if (action.equals("filter_duplicate_list")) {
					new FilterOptionsDialog(application.dataCollection(),new DuplicateListFilter(application.dataCollection()));
				}

				else if (action.equals("filter_combine")) {
					new FilterOptionsDialog(application.dataCollection(),new CombineFilter(application.dataCollection()));
				}
				else if (action.equals("filter_collate")) {
					new FilterOptionsDialog(application.dataCollection(),new CollateListsFilter(application.dataCollection()));
				}
				else if (action.equals("filter_intersect")) {
					new FilterOptionsDialog(application.dataCollection(),new IntersectListsFilter(application.dataCollection()));
				}

			}
			catch (SeqMonkException e) {
				JOptionPane.showMessageDialog(application, e.getMessage(), "Can't run filter", JOptionPane.ERROR_MESSAGE);			
			}
		}
		else if (action.equals("report_vistory")) {
			VistoryDialog.showVistory();
		}
		else if (action.startsWith("report")) {
			if (action != "report_summary" && !application.dataCollection().isQuantitated()) {
				JOptionPane.showMessageDialog(application, "You must quantiate your data before creating reports", "Can't generate report", JOptionPane.ERROR_MESSAGE);			
				return;
			}
			if (action.equals("report_annotated")) {
				new ReportOptions(application,new AnnotatedListReport(application.dataCollection(),application.drawnDataSets()));
			}
			else if (action.equals("report_description")) {
				new ProbeListReportCreator(application.dataCollection().probeSet().getActiveList());
			}

			else if (action.equals("report_group")) {
				new ReportOptions(application,new ProbeGroupReport(application.dataCollection(),application.drawnDataSets()));
			}
			else if (action.equals("report_feature")) {
				new ReportOptions(application,new FeatureReport(application.dataCollection(),application.drawnDataSets()));
			}
			else if (action.equals("report_summary")) {
				new ReportOptions(application,new DataStoreSummaryReport(application.dataCollection(),application.drawnDataSets()));
			}
			else if (action.equals("report_chromosome")) {
				new ReportOptions(application,new ChromosomeViewReport(application.dataCollection(),application.drawnDataSets()));
			}
		}
		else if (action.equals("edit_preferences")) {
			new EditPreferencesDialog(application.dataCollection());
		}
		else if (action.equals("find_feature")) {
			new FindFeatureDialog(application.dataCollection());
		}
		else if (action.equals("find_feature_names")) {
			new FindFeaturesByNameDialog(application.dataCollection());
		}
		else if (action.equals("goto_position")) {
			new GotoDialog(application);
		}
		else if (action.equals("goto_window")) {
			new GotoWindowDialog(application);
		}
		else if (action.equals("copy_position")) {
			Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable t = new StringSelection("chr"+DisplayPreferences.getInstance().getCurrentChromosome().name()+":"+SequenceRead.start(DisplayPreferences.getInstance().getCurrentLocation())+"-"+SequenceRead.end(DisplayPreferences.getInstance().getCurrentLocation()));
			c.setContents(t, null);
		}
		else {
			throw new IllegalStateException("Unknown command '"+action+"' from main menu");
		}
	}



	/**
	 * The Class FileOpener.
	 */
	private class FileOpener implements ActionListener {

		/** The application. */
		private final SeqMonkApplication application;

		/** The file. */
		private final File file;

		/**
		 * Instantiates a new file opener.
		 * 
		 * @param application the application
		 * @param file the file
		 */
		public FileOpener (SeqMonkApplication application, File file) {
			this.application = application;
			this.file = file;
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e) {
			application.loadProject(file);
		}
	}
}
