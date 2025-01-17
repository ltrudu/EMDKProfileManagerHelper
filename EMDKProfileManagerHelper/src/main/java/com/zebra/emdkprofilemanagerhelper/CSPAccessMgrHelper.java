package com.zebra.emdkprofilemanagerhelper;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import java.util.Base64;

public class CSPAccessMgrHelper {
    // TODO: Put your custom certificate in the apkCertificate member for MX AccessMgr registering (only if necessary and if you know what you are doing)
    public static Signature apkCertificate = null;

    public enum EServiceAccessAction
    {
        DoNothing           ("0"),
        //AllowBinding        ("1"),
        //DisallowBinding     ("2"),
        //VerifyBinding       ("3"),
        AllowCaller         ("4"),
        DisallowCaller      ("5");//,
        //VerifyCaller        ("6"),
        //AquireToken         ("7"),
        //VerifyCallerToken   ("8");

            private String name = "";
        EServiceAccessAction(String thename)
        {
            name = thename;
        }

        @Override
        public String toString() {
            return name;
        }

        static EServiceAccessAction fromString(String value)
        {
            switch(value)
            {
                case "4":
                    return AllowCaller;
                case "5":
                    return DisallowCaller;
            }
            return DoNothing;
        }
    }

    public static void executeAccessMgrServiceAccessAction(Context context, String serviceIdentifier, EServiceAccessAction serviceAccessAction, IResultCallbacks callbackInterface) {
        String profileName = "AccessMgr-1";
        String profileData = "";
        try {
           String path = context.getApplicationInfo().sourceDir;
            //final String strAppName = packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
            final String strPackageName = context.getPackageName();

            // Use custom signature if it has been set by the user
            Signature sig = CSPAccessMgrHelper.apkCertificate;

            // Let's check if we have a custom certificate
            if (sig == null) {
                // Nope, we will get the first apk signing certificate that we find
                // You can copy/paste this snippet if you want to provide your own
                // certificate
                // TODO: use the following code snippet to extract your custom certificate if necessary
                final Signature[] arrSignatures;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                    arrSignatures = packageInfo.signingInfo.getApkContentsSigners();
                    if (arrSignatures == null || arrSignatures.length == 0) {
                        if (callbackInterface != null) {
                            callbackInterface.onError("Error : Package has no signing certificates... how's that possible ?","");
                            return;
                        }
                    }
                    sig = arrSignatures[0];
                }
                else
                {
                        PackageManager pm = context.getPackageManager();
                        String packageName = context.getPackageName();
                        PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                        if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
                            if (callbackInterface != null) {
                                callbackInterface.onError("Error : Package has no signatures... how's that possible ?","");
                                return;
                            }
                        }
                        sig = packageInfo.signatures[0];
                }
            }

            /*
             * Get the X.509 certificate.
             */
            final byte[] rawCert = sig.toByteArray();

            // Get the certificate as a base64 string
            String encoded = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                encoded = java.util.Base64.getEncoder().encodeToString(rawCert);
            }
            else
            {
                // Encode the bytes using Base64
                encoded = android.util.Base64.encodeToString(rawCert, android.util.Base64.DEFAULT);
            }

            profileData =
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                            "<characteristic type=\"Profile\">" +
                            "<parm name=\"ProfileName\" value=\"" + profileName + "\"/>" +
                            "<characteristic type=\"AccessMgr\" version=\"11.3\">" +
                            "    <parm name=\"OperationMode\" value=\"1\" />\n" +
                            "    <parm name=\"ServiceAccessAction\" value=\"" + serviceAccessAction.toString() + "\" />\n" +
                            "    <parm name=\"ServiceIdentifier\" value=\"" + serviceIdentifier + "\" />\n" +
                            "    <parm name=\"CallerPackageName\" value=\"" + strPackageName + "\" />\n" +
                            "    <parm name=\"CallerSignature\" value=\"" + encoded + "\" />\n" +
                            "</characteristic>" +
                            "</characteristic>";
            ProfileManagerCommand profileManagerCommand = new ProfileManagerCommand(context);
            profileManagerCommand.execute(profileData, profileName, callbackInterface);
            //}
        } catch (Exception e) {
            e.printStackTrace();
            if (callbackInterface != null) {
                callbackInterface.onError("Error on profile: " + profileName + "\nError:" + e.getLocalizedMessage() + "\nProfileData:" + profileData, "");
            }
        }
    }

}
