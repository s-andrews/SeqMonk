source("http://bioconductor.org/biocLite.R")

# We need to work around a bug in the latest bioconductor/R combination
install.packages("data.table", repos="http://cloud.r-project.org")


install.packages("Rtsne", repos="http://cloud.r-project.org")

biocLite()
biocLite("DESeq2")
biocLite("edgeR")

# NB we don't need to explicity install limma as it's a dependency
# of EdgeR.  If this changes then we'd need to add it.

# If these think they installed OK we now need to test them

library("DESeq2")
library("edgeR")
library("Rtsne")
library("limma")


