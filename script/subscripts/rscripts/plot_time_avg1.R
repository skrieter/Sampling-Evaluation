data.plot = data.agg

factors.model = factor(data.plot$ModelName, unique(data.plot$ModelName))
data.list = split(data.plot, factors.model)

mark.algorithm = "YASA_t2_m10"


data.values = vector()
for(d in data.list) {
	mark = d[which(data.algorithms == mark.algorithm), Time]
	values = vector(mode = "numeric", length = length(data.algorithms))
	for(a in data.algorithms) {
		value = d[which(Algorithm == a), Time]
		if (length(value) == 1) {
			values[which(data.algorithms == a)] = value / mark
		}
	}
	data.values = c(data.values, as.vector(values))
}

data.matrix = matrix(data.values, ncol = length(data.algorithms), byrow=TRUE)


print(data.matrix)
boxplot(log10(data.matrix))
