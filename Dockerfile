# Extend vert.x image
FROM vertx/vertx3

#                                                       
ENV VERTICLE_NAME com.cisco.cmad.vertx_backend.App
ENV VERTICLE_FILE vertx-backend-0.0.1-SNAPSHOT-jar-with-dependencies.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8084

# Copy your verticle to the container                   
COPY $VERTICLE_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]