package com.example.android.digidoor_gate;


public class ScheduledUser {
    private String name;
    private int phoneNumber;
    private int pin;

    public ScheduledUser(){
    }

    public ScheduledUser(String name, int phoneNumber, int pin){
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.pin = pin;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public int getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(int phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getPin(){
        return pin;
    }

    public void setPin(int pin){
        this.pin = pin;
    }


}
