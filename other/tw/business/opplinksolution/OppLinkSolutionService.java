package other.tw.business.opplinksolution;

import com.rkhd.platform.sdk.data.model.OppLinkSolution__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.trigger.DataResult;

import java.util.*;

public class OppLinkSolutionService {

    private final OppLinkSolutionSelector oppLinkSolutionSelector = new OppLinkSolutionSelector();

    public void checkSolutionPercentageBeforeInsert(List<OppLinkSolution__c> newList, Map<OppLinkSolution__c, DataResult> resultMap) throws ApiEntityServiceException {
        Set<Long> oppIds = new HashSet<>();

        for(OppLinkSolution__c oppLinkSolution__c : newList){
            oppIds.add(oppLinkSolution__c.getOpportunity__c());
        }

        Map<Long,Double> oppPercentageMap = new OppLinkSolutionSelector().getOpportunityPercentageMap(oppIds);

        for (OppLinkSolution__c newSolution : newList) {
            Long oppId = newSolution.getOpportunity__c();
            if(!oppPercentageMap.containsKey(oppId)){
                oppPercentageMap.put(oppId, (double) 0);
            }
            double percentage = oppPercentageMap.get(oppId);
            percentage += newSolution.getPercentage__c();
            oppPercentageMap.put(oppId, percentage);
        }

        System.out.println(oppPercentageMap);

        for(OppLinkSolution__c newSolution : newList){
            Long oppId = newSolution.getOpportunity__c();
            if(!oppPercentageMap.get(oppId).equals(1.0)){
                System.out.println(" The total solution percentage does not equal 100%.");
                DataResult dataResult = resultMap.get(newSolution);
                dataResult.setSuccess(false);
                dataResult.setMsg(dataResult.getMsg()+" The total solution percentage does not equal 100%.");
            }
        }
    }

    public void checkSolutionPercentageBeforeUpdate(Map<Long, OppLinkSolution__c> oldMap, Map<Long, OppLinkSolution__c> newMap, Map<OppLinkSolution__c, DataResult> resultMap) throws ApiEntityServiceException {
        Set<Long> oppIds = new HashSet<>();

        for(OppLinkSolution__c oppLinkSolution__c : oldMap.values()){
            oppIds.add(oppLinkSolution__c.getOpportunity__c());
        }
        for(OppLinkSolution__c oppLinkSolution__c : newMap.values()){
            oppIds.add(oppLinkSolution__c.getOpportunity__c());
        }

        Map<Long,Double> oppPercentageMap = oppLinkSolutionSelector.getOpportunityPercentageMap(oppIds);

        for (Long solutionId : newMap.keySet()) {
            OppLinkSolution__c oldSolution = oldMap.get(solutionId);
            OppLinkSolution__c newSolution = newMap.get(solutionId);

            Long oppId1 = oldSolution.getOpportunity__c();
            if(!oppPercentageMap.containsKey(oppId1)){
                oppPercentageMap.put(oppId1, (double) 0);
            }
            double oldPercentage = oppPercentageMap.get(oppId1);
            oldPercentage -= oldSolution.getPercentage__c();
            oppPercentageMap.put(oppId1, oldPercentage);

            Long oppId2 = newSolution.getOpportunity__c();
            if(!oppPercentageMap.containsKey(oppId2)){
                oppPercentageMap.put(oppId2, (double) 0);
            }
            double newPercentage = oppPercentageMap.get(oppId2);
            newPercentage += newSolution.getPercentage__c();
            oppPercentageMap.put(oppId2, newPercentage);
        }

        for (Long solutionId : newMap.keySet()) {
            OppLinkSolution__c oldSolution = oldMap.get(solutionId);
            OppLinkSolution__c newSolution = newMap.get(solutionId);

            Long oldOppId = oldSolution.getOpportunity__c();
            Long newOppId = newSolution.getOpportunity__c();

            if(!(oldOppId.equals(newOppId))){
                if (!oppPercentageMap.get(oldOppId).equals(1.0)) {
                    DataResult dataResult = resultMap.get(newSolution);
                    dataResult.setSuccess(false);
                    dataResult.setMsg(dataResult.getMsg() + " The total solution percentage of old opportunity does not equal 100%.");
                }
            }

            if(!oppPercentageMap.get(newOppId).equals(1.0)){
                DataResult dataResult = resultMap.get(newSolution);
                dataResult.setSuccess(false);
                dataResult.setMsg(dataResult.getMsg()+" The total solution percentage"+(oldOppId.equals(newOppId)?"":" of new opportunity")+" does not equal 100%.");
            }
        }
    }

    public void checkSolutionPercentageBeforeDelete(List<OppLinkSolution__c> deleteList, Map<OppLinkSolution__c, DataResult> resultMap) throws ApiEntityServiceException {
        Set<Long> oppIds = new HashSet<>();

        for(OppLinkSolution__c oppLinkSolution__c : deleteList){
            oppIds.add(oppLinkSolution__c.getOpportunity__c());
        }

        Map<Long,Double> oppPercentageMap = new OppLinkSolutionSelector().getOpportunityPercentageMap(oppIds);

        for (OppLinkSolution__c deleteSolution : deleteList) {
            Long oppId = deleteSolution.getOpportunity__c();
            if(!oppPercentageMap.containsKey(oppId)){
                oppPercentageMap.put(oppId, (double) 0);
            }
            double percentage = oppPercentageMap.get(oppId);
            percentage -= deleteSolution.getPercentage__c();
            oppPercentageMap.put(oppId, percentage);
        }

        for(OppLinkSolution__c deleteSolution : deleteList){
            Long oppId = deleteSolution.getOpportunity__c();
            Double percentage = oppPercentageMap.get(oppId);
            if(!percentage.equals(0.0) && !percentage.equals(1.0)){
                DataResult dataResult = resultMap.get(deleteSolution);
                dataResult.setSuccess(false);
                dataResult.setMsg(dataResult.getMsg()+" The total solution percentage does not equal 100%.");
            }
        }
    }
}
