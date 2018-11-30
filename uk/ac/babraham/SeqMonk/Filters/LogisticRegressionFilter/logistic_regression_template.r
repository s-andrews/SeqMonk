setwd("%%WORKING%%")

list.files(pattern = "data_chr") -> data.files

# Do a sanity check that there is data in every file
# and remove any empty ones

data.files[sapply(data.files,function(x)nrow(read.delim(x,nrows = 2))==2)] -> data.files

process.file <- function(x) {
  
  print (paste("Processing ",x))
  
  read.delim(x,colClasses = c("character", "factor", "factor", "factor", "numeric")) -> data
  
  return(do.call("rbind",(by(data,data$id,logistic.regression))))
  
}

logistic.regression <- function (x) {
  
  fit <- glm(state ~ group+replicate, weights = count, data = x, family = binomial)
  
  rbind(by(x,x[,c("group","replicate")],function(y)y$count[1]/sum(y$count)*100)) -> percentages


  return (c(
    (coef(summary(fit))["groupto", "Pr(>|z|)"]),
    mean(percentages[1,] - percentages[2,]))
  );
  
}

## WARNING ##
## THIS BIT IS SLOW ##

## We do around 250 models/sec ##

as.data.frame(do.call("rbind",lapply(data.files,process.file))) -> regression.results

colnames(regression.results) <- c("P-value","Difference")

p.adjust(regression.results$`P-value`,method = "fdr", n=%%CORRECTCOUNT%%) -> regression.results$FDR

if (%%MULTITEST%%) {
  regression.results[regression.results$FDR < %%PVALUE%%,] -> regression.results
}else {
  regression.results[regression.results$`P-value` < %%PVALUE%%,] -> regression.results
}

# Write the hit names to a file
write.table(regression.results,file="hits.txt",row.names=TRUE,quote=FALSE,sep="\t")
