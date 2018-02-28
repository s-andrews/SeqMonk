setwd("%%WORKING%%")

list.files(pattern = "data_chr") -> data.files

process.file <- function(x) {
  
  print (paste("Processing ",x))
  
  read.delim(x,colClasses = c("character", "factor", "factor", "factor", "numeric")) -> data
  
  return(unlist(as.list(by(data,data$id,logistic.regression))))
  
}

logistic.regression <- function (x) {
  
  fit <- glm(state ~ group+replicate, weights = count, data = x, family = binomial)
  
  return (coef(summary(fit))[2,4])

}

## WARNING ##
## THIS BIT IS SLOW ##

## We do around 250 models/sec ##

unlist(sapply(data.files,process.file)) -> regression.results

# If there's only one input file then we get a matrix with no names, so we need
# to copy in the rownames as this is what we actually need.

if (any(is.null(names(regression.results)))) {
  names(regression.results) <- rownames(regression.results)
} 

if (%%MULTITEST%%) {
	p.adjust(regression.results,method = "fdr") -> regression.results
}

names(regression.results)[regression.results < %%PVALUE%%] -> hit.ids
regression.results[regression.results < %%PVALUE%%] -> p.values

# Write the hit names to a file
write.table(data.frame(id=hit.ids,p.value=p.values),file="hits.txt",row.names=FALSE,quote=FALSE,sep="\t")
