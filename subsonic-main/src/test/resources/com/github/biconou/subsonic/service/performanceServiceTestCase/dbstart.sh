#!/bin/sh

export JAVA_HOME=/software/java/jdk1.8.0_92

export SUBSONIC_HOME=/develop/biconouSubsonic_master/subsonic-main/target/test-classes/com/github/biconou/subsonic/service/performanceServiceTestCase


export DB_HOME=${SUBSONIC_HOME}/db
export DB_DATA_FILE=${DB_HOME}/subsonic
export SUBSONIC_JARS_DIR=${SUBSONIC_HOME}
export DB_CLASS_PATH=${SUBSONIC_JARS_DIR}/hsqldb-1.8.0.7.jar:${SUBSONIC_JARS_DIR}/sqltool-2.3.2.jar

nohup ${JAVA_HOME}/bin/java -cp ${DB_CLASS_PATH} -Xmx100m \
	org.hsqldb.Server -database.0 file:${DB_DATA_FILE} -dbname.0 subsonic \
	> nohup.dbstart.out 2>&1&
