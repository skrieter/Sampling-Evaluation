print("Reading csv files...")
dir.data.parts = list.dirs(path = dir.data.sub, recursive = FALSE)
data.merged <- data.frame()

for (part in dir.data.parts) {
	part.elements = unlist(strsplit(part, "/"))
	part.name = part.elements[length(part.elements)]
	if (!endsWith(part.name, "plots")) {
		print(part.name)
		
		data = read.csv(paste(part, "data/data.csv", sep = "/"), sep = ";", header = TRUE)
		models = read.csv(paste(part, "data/models.csv", sep = "/"), sep = ";", header = TRUE)
		algorithms = read.csv(paste(part, "data/algorithms.csv", sep = "/"), sep = ";", header = TRUE)
	
		#data = data[which(data$SystemIteration == 1),]
		data = data[which(data$InTime == "true"),]
		data = data[which(data$NoError == "true"),]
		data = data[which(data$Valid == "true"),]
		data = data[which(data$Complete == "true"),]
		data = aggregate(data[,c(7,8)], by=list(data$ModelID,data$AlgorithmID,data$SystemIteration), FUN=mean)
		colnames(data)[1] <- "ModelID"
		colnames(data)[2] <- "AlgorithmID"
		colnames(data)[3] <- "SystemIteration"
		
		data = merge(data, algorithms, by="AlgorithmID")
		data = merge(data, models, by="ModelID")
		data = data[,-c(11)]
		data$SystemIteration <- paste(part.name, data$SystemIteration, sep="_")
		colnames(data)[6] <- "AlgorithmName"
		colnames(data)[8] <- "ModelName"
		data <- cbind(data, paste(data$AlgorithmName, data$Settings, sep="_"))
		colnames(data)[11] <- "Algorithm"
				
		data.merged <- rbind(data.merged, data)
	}
}
data.merged <- as.data.table(data.merged[order(data.merged$FMFeatures,data.merged$ModelID,data.merged$AlgorithmID),])
data.agg = data.merged[
	grepl("t2", data.merged$Algorithm, fixed=TRUE) & 
		data.merged$FMFeatures < 10000 & 
		data.merged$FMFeatures > 0, 
	.(Time = mean(Time), Size = mean(Size), Features = max(FMFeatures)), 
	by = .(ModelName, Algorithm)]
data.merged <- as.data.table(data.merged[order(data.merged$FMFeatures,data.merged$ModelID,data.merged$AlgorithmID),])
data.algorithms <- as.vector(unique(data.agg$Algorithm))

print(data.algorithms)
