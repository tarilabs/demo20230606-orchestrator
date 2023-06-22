i=0

### make sure the target jar directory exists
mkdir -p jar-dir

until [ $i -gt 10 ]
do
	### copy the template and set the artifact id
	cat pom-template.xml | sed -e "s/\#\#\#/$i/" > pom.xml

	### run maven
	echo i: $i
	mvn clean install

	### save the generated jar file
	cp target/*.jar jar-dir

	### increment the counter
	((i = i+1))
done
