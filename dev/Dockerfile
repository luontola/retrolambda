FROM centos:7

# Install all supported JDK versions
RUN curl -L --header "Cookie: oraclelicense=accept-securebackup-cookie" \
		http://download.oracle.com/otn-pub/java/jdk/1.5.0_22/jdk-1_5_0_22-linux-amd64-rpm.bin \
		-o jdk-1_5_0_22-linux-amd64-rpm.bin \
	&& chmod u+x jdk-1_5_0_22-linux-amd64-rpm.bin \
	&& echo yes | ./jdk-1_5_0_22-linux-amd64-rpm.bin \
	&& rm -v jdk-1_5_0_22-linux-amd64* \
	&& yum -y install \
		java-1.6.0-openjdk-devel \
		java-1.7.0-openjdk-devel \
		java-1.8.0-openjdk-devel \
	&& yum clean all
ENV JAVA5_HOME=/usr/java/jdk1.5.0_22
ENV JAVA6_HOME=/usr/lib/jvm/java-1.6.0
ENV JAVA7_HOME=/usr/lib/jvm/java-1.7.0
ENV JAVA8_HOME=/usr/lib/jvm/java-1.8.0
ENV JAVA_HOME=$JAVA8_HOME

# Install Maven
RUN curl http://archive.apache.org/dist/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz \
		-o apache-maven-3.2.5-bin.tar.gz \
	&& tar -xzf apache-maven-3.2.5-bin.tar.gz -C /opt \
	&& rm -v apache-maven-3.2.5-bin.tar.gz \
	&& ln -s /opt/apache-maven-3.2.5/bin/mvn /usr/local/bin/ \
	&& mkdir /root/.m2
COPY toolchains.xml /root/.m2/

RUN mkdir /project
WORKDIR /project
