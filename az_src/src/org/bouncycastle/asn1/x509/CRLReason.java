package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.DEREnumerated;

/**
 * The CRLReason enumeration.
 * <pre>
 * CRLReason ::= ENUMERATED {
 *  unspecified             (0),
 *  keyCompromise           (1),
 *  cACompromise            (2),
 *  affiliationChanged      (3),
 *  superseded              (4),
 *  cessationOfOperation    (5),
 *  certificateHold         (6),
 *  removeFromCRL           (8),
 *  privilegeWithdrawn      (9),
 *  aACompromise           (10)
 * }
 * </pre>
 */
public class CRLReason
    extends DEREnumerated
{
    public static final int UNSPECIFIED = 0;
    public static final int KEY_COMPROMISE = 1;
    public static final int CA_COMPROMISE = 2;
    public static final int AFFILIATION_CHANGED = 3;
    public static final int SUPERSEDED = 4;
    public static final int CESSATION_OF_OPERATION  = 5;
    public static final int CERTIFICATE_HOLD = 6;
    public static final int REMOVE_FROM_CRL = 8;
    public static final int PRIVILEGE_WITHDRAWN = 9;
    public static final int AA_COMPROMISE = 10;

    public CRLReason(
        int reason)
    {
        super(reason);
    }

    public CRLReason(
        DEREnumerated reason)
    {
        super(reason.getValue().intValue());
    }
}
