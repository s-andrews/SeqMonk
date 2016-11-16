setwd("%%WORKING%%")

# Read in the full data table from SeqMonk
pca.data <- read.delim("data.txt",quote="",header=FALSE)

prcomp(t(pca.data)) -> pca.results

# Write the PC data to a file
write.table(pca.results$x,file="pca_data.txt",row.names=TRUE,col.names=NA,quote=FALSE,sep="\t")

# Write the variance data to a file
write.table((pca.results$sdev^2),file="variance_data.txt",row.names=TRUE,col.names=NA,quote=FALSE,sep="\t")

# Write out the weightings to a file
write.table(pca.results$rotation,file="pca_weights.txt",row.names=TRUE,col.names=NA,quote=FALSE,sep="\t")

