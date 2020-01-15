library("Cairo")
library("data.table")

source("set_directories.R")
source("functions.R")
source("read.R")

for (i in seq_along(dev.list())) dev.off(dev.cur())

pdfplot("size", 7, 4.66)
pdfplot("time_avg1", 7, 4.66)
pdfplot("size_avg1", 7, 4.66)

source("table_complete.R")
