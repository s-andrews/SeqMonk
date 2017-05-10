setwd("%%WORKING%%")

library(tsne)

# Read in the full data table from SeqMonk
tsne.data <- read.delim("data.txt",quote="",header=FALSE)

tsne(t(pca.data), perplexity=%%PERPLEXITY%%, min_cost=%%CUTOFF%%, epoch=10) -> tsne.results

# Write the TSNE data to a file
write.table(tsne.results,file="tsne_data.txt",row.names=TRUE,col.names=NA,quote=FALSE,sep="\t")


