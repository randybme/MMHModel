package edu.bu.zaman.MMHModel;

import java.util.*;

public class Hospital
{
    public enum MaterialResource
    {
        OXYTOCIN, 
        ATROPINE_SO4, 
        LIGNOCAINE, 
        HYDRALAZINE, 
        HYDROCORTISONE, 
        DIAZEPAM, 
        METRONIDAZOLE_LIQ, 
        DEXAMETHASONE,
        CEFTRIAXONE,
        MG_SO4,
        NACL_DEXTROSE,
        SODIUM_LACTATE_RINGER,
        NACL,
        WATER,
        ADRENALINE,
        DICLOFENAC_NA_LIQ,
        GENTAMYCIN,
        AMOXICILLIN,
        FESO4_FOLIC,
        DICLOFENAC_NA_TAB,
        METRONIDAZOLE_TAB,
        PARACETAMOL,
        METHYLDOPA,
        MISPROSTOL,
        NIFEDIPINE,
        ERYTHROMYCIN,
        ZNO_PLASTER,
        ABSORBENT_GAUZE,
        COTTON_WOOL,
        GLOVES_LATEX,
        GLOVES_SURGICAL,
        CANULAR,
        SYRINGES_2CC,
        SYRINGES_5CC,
        SYRINGES_10CC,
        IV_SET,
        BT_SET,
        BALLOON_CATHETER,
        SPINAL_NEEDLE,
        CHROMIC_CUTGUT_2_0,
        CHROMIC_CUTGUT_2,
        CHROMIC_CUTGUT_1,
        CHROMIC_CUTGUT_2_TAPER,
        SILK_BRAIDED_2,
        SILK_BRAIDED_0,
        URINE_COLLECTION_BAG,
        SCALP_BLADE,
        UMB_CLAMP,
        COATED_PGA,
        DETTOL,
        POVIDINE,
        METH_SPIRIT,
        CHLORINE,
        
    }
    
    /**
     * The number of nurses currently available for monitoring and procedures
     */
    public static int nurses = 0;
    
    /**
     * The number of doctors currently avaiable for procedures
     */
    public static int doctors = 0;
    
    /**
     * Stores the current inventory of disposable material resources
     */
    private static HashMap<MaterialResource, Double> m_totalDisposableResources;
    
    @SuppressWarnings("unused")
    private static Hospital m_instance = new Hospital(); // Eager creation of singleton Hospital object
    private Hospital()
    {
        m_totalDisposableResources = new HashMap<>();
        
        // Load initial inventory of disposable resources
        stockDisposableResource(MaterialResource.OXYTOCIN, 15000);
        stockDisposableResource(MaterialResource.ATROPINE_SO4, 250);
        stockDisposableResource(MaterialResource.LIGNOCAINE, 2500);
        stockDisposableResource(MaterialResource.HYDRALAZINE, 2200);
        stockDisposableResource(MaterialResource.HYDROCORTISONE, 0);
        stockDisposableResource(MaterialResource.DIAZEPAM, 0);
        stockDisposableResource(MaterialResource.METRONIDAZOLE_LIQ, 300000);
        stockDisposableResource(MaterialResource.DEXAMETHASONE, 300);
        stockDisposableResource(MaterialResource.CEFTRIAXONE, 506);
        stockDisposableResource(MaterialResource.MG_SO4, 15000);
        stockDisposableResource(MaterialResource.NACL_DEXTROSE, 60000);
        stockDisposableResource(MaterialResource.SODIUM_LACTATE_RINGER, 600000);
        stockDisposableResource(MaterialResource.NACL, 312000);
        stockDisposableResource(MaterialResource.WATER, 250);
        stockDisposableResource(MaterialResource.ADRENALINE, 0);
        stockDisposableResource(MaterialResource.DICLOFENAC_NA_LIQ, 4200);
        stockDisposableResource(MaterialResource.GENTAMYCIN, 0);
        stockDisposableResource(MaterialResource.AMOXICILLIN, 500000);
        stockDisposableResource(MaterialResource.FESO4_FOLIC, 200000);
        stockDisposableResource(MaterialResource.DICLOFENAC_NA_TAB, 50000);
        stockDisposableResource(MaterialResource.METRONIDAZOLE_TAB, 2000000);
        stockDisposableResource(MaterialResource.PARACETAMOL, 2000000);
        stockDisposableResource(MaterialResource.METHYLDOPA, 700000);
        stockDisposableResource(MaterialResource.NIFEDIPINE, 0);
        stockDisposableResource(MaterialResource.ERYTHROMYCIN, 0);
        stockDisposableResource(MaterialResource.ZNO_PLASTER, 0);
        stockDisposableResource(MaterialResource.ABSORBENT_GAUZE, 2000);
        stockDisposableResource(MaterialResource.COTTON_WOOL, 0);
        stockDisposableResource(MaterialResource.GLOVES_LATEX, 12000);
        stockDisposableResource(MaterialResource.GLOVES_SURGICAL, 4000);
        stockDisposableResource(MaterialResource.CANULAR, 500);
        stockDisposableResource(MaterialResource.SYRINGES_2CC, 1800);
        stockDisposableResource(MaterialResource.SYRINGES_5CC, 2800);
        stockDisposableResource(MaterialResource.SYRINGES_10CC, 0);
        stockDisposableResource(MaterialResource.IV_SET, 1000);
        stockDisposableResource(MaterialResource.BT_SET, 0);
        stockDisposableResource(MaterialResource.BALLOON_CATHETER, 600);
        stockDisposableResource(MaterialResource.SPINAL_NEEDLE, 250);
        stockDisposableResource(MaterialResource.CHROMIC_CUTGUT_2_0, 600);
        stockDisposableResource(MaterialResource.CHROMIC_CUTGUT_2, 0);
        stockDisposableResource(MaterialResource.CHROMIC_CUTGUT_1, 360);
        stockDisposableResource(MaterialResource.CHROMIC_CUTGUT_2_TAPER, 420);
        stockDisposableResource(MaterialResource.SILK_BRAIDED_2, 60);
        stockDisposableResource(MaterialResource.SILK_BRAIDED_0, 120);
        stockDisposableResource(MaterialResource.URINE_COLLECTION_BAG, 250);
        stockDisposableResource(MaterialResource.SCALP_BLADE, 500);
        stockDisposableResource(MaterialResource.UMB_CLAMP, 1000);
        stockDisposableResource(MaterialResource.COATED_PGA, 600);
        stockDisposableResource(MaterialResource.DETTOL, 10);
        stockDisposableResource(MaterialResource.POVIDINE, 40);
        stockDisposableResource(MaterialResource.METH_SPIRIT, 15);
        stockDisposableResource(MaterialResource.CHLORINE, 5);
        
    }
    
    /**
     * Provides the specified amount of a disposable resource to the hospital to stock
     * in it's disposable resource inventory.
     * 
     * @param resource  the disposable resource to stock
     * @param amount    the amount (in mg) to stock for the disposable resource
     */
    public static void stockDisposableResource(MaterialResource resource, double amount)
    {
        // Check to make sure the amount is positive
        if (amount <= 0)
        {
            return;
        }
        
        // Update the inventory for the specified resource depending on whether or not
        // the specified resource is already in stock
        if (m_totalDisposableResources.containsKey(resource))
        {
            double currentAmount = m_totalDisposableResources.get(resource);
            m_totalDisposableResources.put(resource, currentAmount + amount);
        }
        else
        {
            m_totalDisposableResources.put(resource, amount);
        }
    }
    
    /**
     * Returns whether the specified resource is available at the requested dose
     *
     * @param resource  the disposable resource being requested
     * @param dose      the amount (in mg) that is being requested
     */
    public static boolean isAvailable(MaterialResource resource, double dose)
    {
        if (!m_totalDisposableResources.containsKey(resource))
        {
            return false;
        }
        
        return (m_totalDisposableResources.get(resource) >= dose);
    }
    
    /**
     * Requests and consumes the specified dose of a particular material resource from the
     * hospital stock.
     *
     * @param resource  the disposable resource being requested
     * @param dose      the amount (in mg) that is being requested and consumed
     */
    public static void consumeResource(MaterialResource resource, double dose)
    {
        // Check to make sure the disposable resource being requested is in stock
        if (!isAvailable(resource, dose))
        {
            return;
        }
        
        double currentAmount = m_totalDisposableResources.get(resource);
        currentAmount -= dose;
        
        m_totalDisposableResources.put(resource, currentAmount);
    }
    public static HashMap<MaterialResource, Double> getHospitalStock() {
    	return m_totalDisposableResources;
    }
    public static void timeCheck(int cycle) {
    	if (cycle%16 == 0)
    	{
    		Hospital.nurses = 5;
    		Hospital.doctors = 3;
    	}
    	else if ((cycle%8 == 0) && (cycle%16 != 0))
    	{
    		Hospital.doctors = 1;
    	}
    	else if (cycle%4 == 0 && (cycle%8 != 0) && (cycle%16 != 0))
    	{
    		Hospital.nurses = 3;
    		Hospital.doctors = 2;
    	}
    	
    	if (cycle%10 == 0) {
    		stockDisposableResource(MaterialResource.OXYTOCIN, 15000);
            stockDisposableResource(MaterialResource.HYDRALAZINE, 2200);
            stockDisposableResource(MaterialResource.MG_SO4, 15000);
           
    	}
    }
}
