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

1. Install the 64-bit [Java Runtime Environment](https://java.com/en/download/windows-64bit.jsp)
2. Install [R](https://cran.r-project.org/bin/windows/base/)
3. Download the latest SeqMonk zip file from the [project website](http://www.bioinformatics.babraham.ac.uk/projects/download.html#seqmonk)
4. Unzip the contents somewhere on your machine
5. Double click on seqmonk.exe

# OSX

1. Install the 64-bit [Java Development Kit](http://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html). Note that the Java Runtime Environment is **NOT** enough.  It has to be the development kit.
2. Install [R](https://cran.r-project.org/bin/macosx/)
3. Install the [R OSX toolchain](https://github.com/coatless/r-macos-rtools) to allow you to compile all of the packages you will need
4. Download the latest SeqMonk DMG file from the [project website](http://www.bioinformatics.babraham.ac.uk/projects/download.html#seqmonk)
5. Open the DMG file and drag the icon to somewhere on your computer (normally the applications folder).
6. Double click the icon.  If you get a warning about an unsigned application then right-click on the icon and select "Open". You should then be able to allow the application to run.  This warning will only appear once per-machine.

# Linux

1. Install the 64-bit version of openJRE. (Ubuntu: sudo apt install default-jre    Centos: sudo yum install java-1.8.0-openjdk)
2. Install [R](https://cran.r-project.org/bin/linux/)
3. Install a development environment (Ubuntu: sudo apt install build-essential  Centos: sudo yum groupinstall "Development tools")
4. Install the xml2-dev library (Ubuntu: sudo apt install libxml2-dev  Centos: sudo yum install libxml2-devel)
5. Download the latest SeqMonk zip file from the [project website](http://www.bioinformatics.babraham.ac.uk/projects/download.html#seqmonk)
6. Unzip the contents somewhere on your machine
7. Use cd to move to the directory you just created
8. Make the launcher executable with chmod 755 seqmonk
9. Run ./seqmonk
