package other.tw.business;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {

    public static final Integer AMENDATORY_TYPE_ADDITIONAL_TCV = 1;
    public static final Integer AMENDATORY_TYPE_OVERWRITE_TVC = 2;

    public static final Integer ACCOUNT_APPROVAL_STATUS = 3;

    public static final Long CONTRACT_MSA_ENTITY_TYPE_ID = 11010001500001L;
    public static final Long CONTRACT_SOW_ENTITY_TYPE_ID = 4065665295128021L;
    public static final Long CONTRACT_PO_ENTITY_TYPE_ID = 4065661041152796L;
    public static final Long CONTRACT_OTHER_ENTITY_TYPE_ID = 4065666220348162L;

    public static final String OPPORTUNITY_CLOSED_WON_STAGE = "Closed Won";
    public static final Set<Long> OPPORTUNITY_SALESTAGE_CONTRACT_NEGOTIATION_IDSET = new HashSet<>(Arrays.asList(3994468391704029L, 4087333019506116L));
    public static final Set<Long> OPPORTUNITY_SALESTAGE_CLOSED_WON_IDSET = new HashSet<>(Arrays.asList(3994468399698710L, 4087333019506117L));


}
