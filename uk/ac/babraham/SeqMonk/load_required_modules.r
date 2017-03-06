source("http://bioconductor.org/biocLite.R")

# We need to work around a bug in the latest bioconductor/R combination
install.packages("data.table", repos="http://cloud.r-project.org")

biocLite()
biocLite("DESeq2")
biocLite("edgeR")

# If these think they installed OK we now need to test them

library("DESeq2")
library("edgeR")


