setwd("%%WORKING%%")

library(Rtsne)

# Read in the full data table from SeqMonk
tsne.data <- read.delim("data.txt",quote="",header=FALSE)

Rtsne(t(tsne.data), perplexity=%%PERPLEXITY%%, max_iter=%%ITERATIONS%%, verbose=TRUE, check_duplicates=FALSE) -> tsne.results

# Write the TSNE data to a file
write.table(tsne.results$Y,file="tsne_data.txt",row.names=TRUE,col.names=NA,quote=FALSE,sep="\t")


