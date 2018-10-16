setwd("%%WORKING%%")

# Read in the quantitations from SeqMonk. We only need the first column
seqmonk.quantitation <- read.delim("quantitation.txt")[[1]]

# Load the segmentation library
library(fastseg)

# Perform the analysis.
fastseg(seqmonk.quantitation,type=%%GLOBAL%%) -> segments

# Write the segments to a file
write.table(segments,file="segments.txt",row.names=FALSE,quote=FALSE,sep="\t")
