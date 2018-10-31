source("http://bioconductor.org/biocLite.R")

# We need to work around a bug in the latest bioconductor/R combination
install.packages("data.table", repos="http://cloud.r-project.org")

# EdgeR also has a buggy manifest which doesn't say it needs the 
# statmod package, so we get that too.
install.packages("statmod", repos="http://cloud.r-project.org")


install.packages("Rtsne", repos="http://cloud.r-project.org")

biocLite()
biocLite("DESeq2")
biocLite("edgeR")
biocLite("fastseg")

# NB we don't need to explicity install limma as it's a dependency
# of EdgeR.  If this changes then we'd need to add it.

# If these think they installed OK we now need to test them

library("DESeq2")
library("edgeR")
library("Rtsne")
library("limma")
library("statmod")
library("fastseg")

