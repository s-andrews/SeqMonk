setwd("%%WORKING%%")

# Read in the full count table from SeqMonk
just.raw.counts <- read.delim("counts.txt",quote="",row.names=1)

# Make up a condition table
column.data <- data.frame(%%CONDITIONS%%)

rownames(column.data) <- colnames(just.raw.counts)

# Load DESeq2
library("DESeq2")

# Make a DESeq data set from the counts and the design and specify which factors in
# the design to test
count.data.set <- DESeqDataSetFromMatrix(countData=just.raw.counts, colData=column.data,design= ~ %%DESIGN%%)

# Perform the analysis.
if(length(levels(column.data$source))>2) {
  count.data.set <- DESeq(count.data.set, test="LRT", full= ~%%DESIGN%%, reduced= ~%%REDUCED%%)
}else {
  count.data.set <- DESeq(count.data.set)
}

# Retrieve the full set of results.
binomial.result <- results(count.data.set,independentFiltering=%%INDEPENDENT%%)

# Remove unmeasured results
na.omit(binomial.result) -> binomial.result

# Select significant hits
binomial.result[binomial.result$%%CORRECTED%% <= %%PVALUE%%,] -> significant.results

# Order the hits by p-value
significant.results[order(significant.results$%%CORRECTED%%),] -> significant.results

# Write the hit names to a file
write.table(significant.results,file="hits.txt",row.names=TRUE,col.names=NA,quote=FALSE,sep="\t")
