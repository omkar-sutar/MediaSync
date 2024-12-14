package com.dubu.MediaSync;

public class CustomExceptions {
    public static class ConnectionError extends Throwable{
        public ConnectionError(String errorMessage,Throwable err){
            super(errorMessage,err);
        }
        public ConnectionError(String errorMessage){
            super(errorMessage);
        }
    }
    public static class UserUnauthorizedException extends Exception{

        public UserUnauthorizedException() {
            super("Unauthorized");
        }
        public UserUnauthorizedException(String message){
            super(message);
        }
    }
}
