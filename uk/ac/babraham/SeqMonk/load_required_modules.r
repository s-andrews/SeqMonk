# We need to specify a repository to use.  If the user has one set
# in their preferences then we'll use that.  If not then we'll use
# the generic cloud R URL.

repository = getOption("CRAN","http://cloud.r-project.org")


# We want to force windows and mac users to use binary distributions
# to do this we need to set the type value for install.packages. We 
# need to look at the OS to figure out what this should be

if (Sys.info()["sysname"] == "Windows") {
  options(install.packages.check.source = "no")
}

if (Sys.info()["sysname"] == "Darwin") {
  options(install.packages.check.source = "no")
}

install.cran <- function (package) {
  
  # First test if it's here already
  if (!package %in% rownames(installed.packages())) {
    install.packages(package, repos=repository)
  }else {
    print(paste0(package," is already installed"))
  }
  
}

install.bioconductor <- function (package) {
  
  # First test if it's here already
  if (!package %in% rownames(installed.packages())) {
    BiocManager::install(package, update=FALSE)
  }else {
    print(paste0(package," is already installed"))
  }
  
}



# We need to work around a bug in the latest bioconductor/R combination
install.cran("data.table")

# EdgeR also has a buggy manifest which doesn't say it needs the 
# statmod package, so we get that too.
install.cran("statmod")


install.cran("Rtsne")



# We used to support both the old and new versions of bioconductor
# but since the R package repository for versions < 3.6 has been 
# broken for months we're now going to insist on R 3.6 and therefore
# the newer version of Bioconductor using BiocManager.

  
install.cran("BiocManager")
install.bioconductor("DESeq2")
install.bioconductor("edgeR")
install.bioconductor("fastseg")

# NB we don't need to explicitly install limma as it's a dependency
# of EdgeR.  If this changes then we'd need to add it.

# If these think they installed OK we now need to test them

library("DESeq2")
library("edgeR")
library("Rtsne")
library("limma")
library("statmod")
library("fastseg")

