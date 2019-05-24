# SeqMonk
SeqMonk is a desktop application for the visualisation and analysis of large mapped genomic datasets, normally coming from high througput sequencing.

![SeqMonk Screenshot](http://www.bioinformatics.babraham.ac.uk/projects/seqmonk/seqmonk.png)

It is useful for the analysis of any experiment type which produces sets of mapped positions relating to a reference genome.  This would include (but is not limited to)

* RNA-Seq
* ChIP-Seq
* BS-Seq
* HiC

This project page contains the source code for the application and is useful only to people wanting to develop new functionality in SeqMonk.  If you just want to run the program then you want to go to the [**project web page**](http://www.bioinformatics.babraham.ac.uk/projects/seqmonk/) where you can download the normal desktop pacakges for Windows, OSX and Linux.

# Installation Instructions

# Windows

1. Install [R](https://cran.r-project.org/bin/windows/base/)
2. Download the latest SeqMonk windows zip file from the [project website](http://www.bioinformatics.babraham.ac.uk/projects/download.html#seqmonk)
3. Unzip the contents somewhere on your machine
4. Double click on seqmonk.exe

# OSX

1. Install [R](https://cran.r-project.org/bin/macosx/)
2. Install the [R OSX toolchain](https://github.com/rmacoslib/r-macos-rtools) to allow you to compile all of the packages you will need
3. Download the latest SeqMonk DMG file from the [project website](http://www.bioinformatics.babraham.ac.uk/projects/download.html#seqmonk)
4. Open the DMG file and drag the icon to somewhere on your computer (normally the applications folder).
5. Double click the icon.  If you get a warning about an unsigned application then right-click on the icon and select "Open". You should then be able to allow the application to run.  This warning will only appear once per-machine.

# Linux

1. Install [R](https://cran.r-project.org/bin/linux/)
2. Install a development environment (Ubuntu: sudo apt install build-essential  Centos: sudo yum groupinstall "Development tools")
3. Install the xml2-dev library (Ubuntu: sudo apt install libxml2-dev  Centos: sudo yum install libxml2-devel)
4. Download the latest SeqMonk linux tar.gz file from the [project website](http://www.bioinformatics.babraham.ac.uk/projects/download.html#seqmonk)
5. Untar the contents somewhere on your machine
6. Use cd to move to the directory you just created
7. Run ./seqmonk
