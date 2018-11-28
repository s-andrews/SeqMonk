setwd("%%WORKING%%")

# Read in the full count table from SeqMonk
just.raw.counts <- read.delim("counts.txt",quote="",row.names=1)

# Make up a condition table
group <- c(%%CONDITIONS%%)


library("edgeR")

edger.data <- DGEList(counts=just.raw.counts,group=group)

edger.data <- calcNormFactors(edger.data)
edger.data <- estimateCommonDisp(edger.data)
edger.data <- estimateTagwiseDisp(edger.data)
test.results <- exactTest(edger.data)

corrected.results <- topTags(test.results,n=nrow(test.results$table))

write.table(corrected.results$table[corrected.results$table$%%CORRECTED%% <= %%PVALUE%%,],file="hits.txt",row.names=TRUE,col.names=NA,quote=FALSE,sep="\t")



