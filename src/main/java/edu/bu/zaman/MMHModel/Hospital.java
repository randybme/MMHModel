package edu.bu.zaman.MMHModel;

import java.util.*;

public class Hospital
{
    public enum MaterialResource
    {
        OXYTOCIN, MG_SO4, HYDROLAZINE
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
        stockDisposableResource(MaterialResource.OXYTOCIN, 100);
        stockDisposableResource(MaterialResource.MG_SO4, 100);
        stockDisposableResource(MaterialResource.HYDROLAZINE, 100);
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
}
