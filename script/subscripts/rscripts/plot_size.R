plot(0,0, xlim = range(0,max(data.agg$Features)), ylim = range(0,max(data.agg$Size)),
	type = "n", 
 	xlab = "",
 	ylab="")

algo.colors = colors(length(data.algorithms)+1)

for (i in seq_along(data.algorithms)) {
	d = data.agg[data.agg$Algorithm==data.algorithms[i],,]
	points(d$Features, d$Size, pch = i, lwd=2, col=algo.colors[i])
	print(d)
}
mtext("t", side = 1, line = 2)
mtext("Testing effectiveness relative to t (%)", side = 2, line = 2.6)

legend("topleft", ncol=2, inset=.05,legend=data.algorithms, col=algo.colors, lwd=2, lty=0, pch=seq_along(data.algorithms), cex=1,
      title="     Algorithms     ")
