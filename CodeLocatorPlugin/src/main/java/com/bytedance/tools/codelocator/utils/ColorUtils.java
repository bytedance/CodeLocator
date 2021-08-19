package com.bytedance.tools.codelocator.utils;

import java.awt.*;

public class ColorUtils {

    private static double[] lab2xyz(double[] lab) {
        double[] xyz = new double[3];
        double l, a, b;
        double fx, fy, fz;
        double xn, yn, zn;
        xn = 95.04;
        yn = 100;
        zn = 108.89;

        l = lab[0];
        a = lab[1];
        b = lab[2];

        fy = (l + 16) / 116;
        fx = a / 500 + fy;
        fz = fy - b / 200;

        if (fx > 0.2069) {
            xyz[0] = xn * Math.pow(fx, 3);
        } else {
            xyz[0] = xn * (fx - 0.1379) * 0.1284;
        }

        if ((fy > 0.2069) || (l > 8)) {
            xyz[1] = yn * Math.pow(fy, 3);
        } else {
            xyz[1] = yn * (fy - 0.1379) * 0.1284;
        }

        if (fz > 0.2069) {
            xyz[2] = zn * Math.pow(fz, 3);
        } else {
            xyz[2] = zn * (fz - 0.1379) * 0.1284;
        }

        return xyz;
    }

    private static double[] xyz2lab(double[] xyz) {
        double[] lab = new double[3];
        double x, y, z;
        x = xyz[0];
        y = xyz[1];
        z = xyz[2];
        double xn, yn, zn;
        xn = 95.04;
        yn = 100;
        zn = 108.89;
        double xxn, yyn, zzn;
        xxn = x / xn;
        yyn = y / yn;
        zzn = z / zn;

        double fx, fy, fz;

        if (xxn > 0.008856) {
            fx = Math.pow(xxn, 0.333333);
        } else {
            fx = 7.787 * xxn + 0.137931;
        }

        if (yyn > 0.008856) {
            fy = Math.pow(yyn, 0.333333);
        } else {
            fy = 7.787 * yyn + 0.137931;
        }

        if (zzn > 0.008856) {
            fz = Math.pow(zzn, 0.333333);
        } else {
            fz = 7.787 * zzn + 0.137931;
        }

        lab[0] = 116 * fy - 16;
        lab[1] = 500 * (fx - fy);
        lab[2] = 200 * (fy - fz);

        return lab;
    }

    private static double[] rgb2xyz(int[] rgb) {
        double[] xyz = new double[3];
        double r, g, b;
        r = rgb[0];
        g = rgb[1];
        b = rgb[2];
        r /= 255;
        g /= 255;
        b /= 255;

        if (r <= 0.04045) {
            r = r / 12.92;
        } else {
            r = Math.pow(((r + 0.055) / 1.055), 2.4);
        }

        if (g <= 0.04045) {
            g = g / 12.92;
        } else {
            g = Math.pow(((g + 0.055) / 1.055), 2.4);
        }

        if (b <= 0.04045) {
            b = b / 12.92;
        } else {
            b = Math.pow(((b + 0.055) / 1.055), 2.4);
        }

        xyz[0] = 41.24 * r + 35.76 * g + 18.05 * b;
        xyz[1] = 21.26 * r + 71.52 * g + 7.2 * b;
        xyz[2] = 1.93 * r + 11.92 * g + 95.05 * b;

        return xyz;
    }

    private static int[] xyz2rgb(double[] xyz) {
        int[] rgb = new int[3];
        double x, y, z;
        double dr, dg, db;
        x = xyz[0];
        y = xyz[1];
        z = xyz[2];

        dr = 0.032406 * x - 0.015371 * y - 0.0049895 * z;
        dg = -0.0096891 * x + 0.018757 * y + 0.00041914 * z;
        db = 0.00055708 * x - 0.0020401 * y + 0.01057 * z;

        if (dr <= 0.00313) {
            dr = dr * 12.92;
        } else {
            dr = Math.exp(Math.log(dr) / 2.4) * 1.055 - 0.055;
        }

        if (dg <= 0.00313) {
            dg = dg * 12.92;
        } else {
            dg = Math.exp(Math.log(dg) / 2.4) * 1.055 - 0.055;
        }

        if (db <= 0.00313) {
            db = db * 12.92;
        } else {
            db = Math.exp(Math.log(db) / 2.4) * 1.055 - 0.055;
        }

        dr = dr * 255;
        dg = dg * 255;
        db = db * 255;

        dr = Math.min(255, dr);
        dg = Math.min(255, dg);
        db = Math.min(255, db);

        rgb[0] = (int) (dr + 0.5);
        rgb[1] = (int) (dg + 0.5);
        rgb[2] = (int) (db + 0.5);

        return rgb;
    }

    /**
     * Calculate the colour difference value between two colours in lab space.
     *
     * @param L1 first colour's L component
     * @param a1 first colour's a component
     * @param b1 first colour's b component
     * @param L2 second colour's L component
     * @param a2 second colour's a component
     * @param b2 second colour's b component
     * @return the CIE 2000 colour difference
     */
    private static double calculateDeltaE(double L1, double a1, double b1, double L2, double a2, double b2) {
        double Lmean = (L1 + L2) / 2.0; //ok
        double C1 = Math.sqrt(a1 * a1 + b1 * b1); //ok
        double C2 = Math.sqrt(a2 * a2 + b2 * b2); //ok
        double Cmean = (C1 + C2) / 2.0; //ok

        double G = (1 - Math.sqrt(Math.pow(Cmean, 7) / (Math.pow(Cmean, 7) + Math.pow(25, 7)))) / 2; //ok
        double a1prime = a1 * (1 + G); //ok
        double a2prime = a2 * (1 + G); //ok

        double C1prime = Math.sqrt(a1prime * a1prime + b1 * b1); //ok
        double C2prime = Math.sqrt(a2prime * a2prime + b2 * b2); //ok
        double Cmeanprime = (C1prime + C2prime) / 2; //ok

        double h1prime = Math.atan2(b1, a1prime) + 2 * Math.PI * (Math.atan2(b1, a1prime) < 0 ? 1 : 0);
        double h2prime = Math.atan2(b2, a2prime) + 2 * Math.PI * (Math.atan2(b2, a2prime) < 0 ? 1 : 0);
        double Hmeanprime = ((Math.abs(h1prime - h2prime) > Math.PI) ? (h1prime + h2prime + 2 * Math.PI) / 2 : (h1prime + h2prime) / 2); //ok

        double T = 1.0 - 0.17 * Math.cos(Hmeanprime - Math.PI / 6.0) + 0.24 * Math.cos(2 * Hmeanprime) + 0.32 * Math.cos(3 * Hmeanprime + Math.PI / 30) - 0.2 * Math.cos(4 * Hmeanprime - 21 * Math.PI / 60); //ok

        double deltahprime = ((Math.abs(h1prime - h2prime) <= Math.PI) ? h2prime - h1prime : (h2prime <= h1prime) ? h2prime - h1prime + 2 * Math.PI : h2prime - h1prime - 2 * Math.PI); //ok

        double deltaLprime = L2 - L1; //ok
        double deltaCprime = C2prime - C1prime; //ok
        double deltaHprime = 2.0 * Math.sqrt(C1prime * C2prime) * Math.sin(deltahprime / 2.0); //ok
        double SL = 1.0 + ((0.015 * (Lmean - 50) * (Lmean - 50)) / (Math.sqrt(20 + (Lmean - 50) * (Lmean - 50)))); //ok
        double SC = 1.0 + 0.045 * Cmeanprime; //ok
        double SH = 1.0 + 0.015 * Cmeanprime * T; //ok

        double deltaTheta = (30 * Math.PI / 180) * Math.exp(-((180 / Math.PI * Hmeanprime - 275) / 25) * ((180 / Math.PI * Hmeanprime - 275) / 25));
        double RC = (2 * Math.sqrt(Math.pow(Cmeanprime, 7) / (Math.pow(Cmeanprime, 7) + Math.pow(25, 7))));
        double RT = (-RC * Math.sin(2 * deltaTheta));

        double KL = 1;
        double KC = 1;
        double KH = 1;

        double deltaE = Math.sqrt(
                ((deltaLprime / (KL * SL)) * (deltaLprime / (KL * SL))) +
                        ((deltaCprime / (KC * SC)) * (deltaCprime / (KC * SC))) +
                        ((deltaHprime / (KH * SH)) * (deltaHprime / (KH * SH))) +
                        (RT * (deltaCprime / (KC * SC)) * (deltaHprime / (KH * SH)))
        );
        return deltaE;
    }

    public static double calculateColorDistance(Color color1, Color color2) {
        int[] color1Rgb = new int[3];
        color1Rgb[0] = color1.getRed();
        color1Rgb[1] = color1.getGreen();
        color1Rgb[2] = color1.getBlue();

        int[] color2Rgb = new int[3];
        color2Rgb[0] = color2.getRed();
        color2Rgb[1] = color2.getGreen();
        color2Rgb[2] = color2.getBlue();

        final double[] color1lab = xyz2lab(rgb2xyz(color1Rgb));
        final double[] color2lab = xyz2lab(rgb2xyz(color2Rgb));

        return calculateDeltaE(color1lab[0], color1lab[1], color1lab[2], color2lab[0], color2lab[1], color2lab[2]);
    }

}
