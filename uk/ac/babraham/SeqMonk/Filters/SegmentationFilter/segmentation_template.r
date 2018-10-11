setwd("%%WORKING%%")

# Read in the full count table from SeqMonk
just.raw.counts <- read.delim("counts.txt",quote="",row.names=1)

# Make up a condition table
conditions <- as.factor(c(%%CONDITIONS%%))

design <- data.frame(All=rep(1,length(conditions)),To=as.numeric(conditions=="to"))

# Load Limma
library("limma")

# Perform the analysis.
lmFit(just.raw.counts,design) -> fit

eBayes(fit,trend=TRUE) -> fit

topTable(fit,coef="To",number=nrow(just.raw.counts),adjust.method="BH") -> all.hits

# Select significant hits
all.hits[all.hits$%%CORRECTED%% <= %%PVALUE%%,] -> significant.results

# Write the hit names to a file
write.table(significant.results,file="hits.txt",row.names=TRUE,col.names=NA,quote=FALSE,sep="\t")
