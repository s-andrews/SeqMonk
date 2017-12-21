setwd("%%WORKING%%")

# Read in the full count table from SeqMonk
just.raw.counts <- read.delim("counts.txt",quote="",row.names=1)

# Make up a condition table
group <- c(%%CONDITIONS%%)


library("edgeR")

edger.data <- DGEList(counts=just.raw.counts,group=group)

totalCount <- colMeans(matrix(edger.data$samples$lib.size, nrow=2, ncol=length(group)/2))

edger.data$samples$lib.size <- rep(totalCount, each=2)

samples <- unique(gsub("_unmeth","",gsub("_meth","",colnames(just.raw.counts))))

design <- data.frame(Int=rep(1,length(group)))

for (index in 2:length(samples)) {
  cbind(design,c(rep(0,(index-1)*2),1,1,rep(0,(length(samples)-index)*2))) -> design
}

design <- cbind(design,rep(c(1,0),length(samples)))

design <- cbind(design,c(
  rep(c(0,0),length(grep("from",samples))),
  rep(c(1,0),length(grep("to",samples))))
)

colnames(design) <- c("Int",samples[2:length(samples)],"Me1","Me2")


edger.data <- estimateDisp(edger.data, design=design, trend="none", robust=TRUE)

fit <- glmFit(edger.data, design)

contr <- makeContrasts(BvsL=Me2, levels=design)

lrt.results <- glmLRT(fit, contrast=contr)

corrected.results <- topTags(lrt.results,n=nrow(lrt.results$table))

write.table(corrected.results$table[corrected.results$table$%%CORRECTED%% < %%PVALUE%%,],file="hits.txt",row.names=TRUE,col.names=NA,quote=FALSE,sep="\t")



