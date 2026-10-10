package com.blueninjas24.civilizations.industry;

import java.math.BigInteger;

/** Conserved owner investment, not a grant or personal-wallet model. Limits apply to actual saved balances. */
public record BusinessPurchasePolicy(long retainedBusinessHundredths,int businessPercent,int estatePercent) {
    public BusinessPurchasePolicy {
        if(retainedBusinessHundredths<0||retainedBusinessHundredths>1_000_000_000_000L
                ||businessPercent<0||businessPercent>100||estatePercent<0||estatePercent>100)
            throw new IllegalArgumentException("Invalid business purchase funding limits");
    }
    public long businessAvailable(long cash){
        if(cash<0)throw new IllegalArgumentException("Negative investment cash");
        return portion(Math.max(0,cash-retainedBusinessHundredths),businessPercent);
    }
    public long estateAvailable(long cash){return portion(cash,estatePercent);}
    private static long portion(long cash,int percent){
        if(cash<0)throw new IllegalArgumentException("Negative investment cash");
        return BigInteger.valueOf(cash).multiply(BigInteger.valueOf(percent)).divide(BigInteger.valueOf(100)).longValueExact();
    }
    public static BusinessPurchasePolicy prototype(){return new BusinessPurchasePolicy(1000,25,100);}
    public static BusinessPurchasePolicy disabled(){return new BusinessPurchasePolicy(0,0,0);}
}
