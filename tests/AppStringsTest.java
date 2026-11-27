package com.clenova.app;
public final class AppStringsTest {
 public static void main(String[] args){
  String[] langs={"hi","es","fr","de"};
  String[] expected={"सेटिंग्स","Ajustes","Réglages","Einstellungen"};
  for(int i=0;i<langs.length;i++){
   if(!AppStrings.translate(langs[i],"Settings").equals(expected[i]))throw new AssertionError("Settings "+langs[i]);
   for(String label:new String[]{"Appearance","Language","Notifications","Light","Dark","System","Agent updates","Cleanup reminders","Storage alerts","Review cleanup","Available storage"}){
    String translated=AppStrings.translate(langs[i],label);
    if(translated==null||translated.isEmpty())throw new AssertionError(label);
   }
  }
  if(!AppStrings.translate("en","Settings").equals("Settings"))throw new AssertionError();
  if(!AppStrings.translate("invalid","Settings").equals("Settings"))throw new AssertionError();
  if(!AppStrings.translate("hi","4.8 GB").equals("4.8 GB"))throw new AssertionError();
  System.out.println("Preference translation checks passed");
 }
}
