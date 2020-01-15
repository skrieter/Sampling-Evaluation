pdfplot <- function(name, w, h, pdfName = name) {
  fileName = paste(c(dir.output, "/", pdfName, ".pdf"), collapse="")
  sourceName = paste(c(name, ".R"), collapse="")
  CairoPDF(fileName, width = w, height = h)
  par(oma=c(0,0,0.2,0))
  par(mar=c(3.5,3.5,0,0))
  source(sourceName)
  dev.off()
}

algo.colors <- rgb(c(215,253,225,171,44,0),
                   c(25,174,225,217,123,0),
                   c(28,97,191,233,182,255),
                   max = 255)

colors <- colorRampPalette(c("black", "white"))