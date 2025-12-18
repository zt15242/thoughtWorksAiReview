package other.tw.business.util;

import com.rkhd.platform.sdk.model.XObject;

import java.lang.reflect.Field;
import java.util.*;

public class CollectionUtils {

    public static <T extends XObject> Map<Long, Double> aggregateFieldByGroup(List<T> records, String groupByField, String aggregateField) {
        Map<Long, Double> aggregatedMap = new HashMap<>();

        for (T record : records) {
            Long groupByFieldValue = record.getAttribute(groupByField);
            Double aggregateFieldValue = record.getAttribute(aggregateField);

            if (aggregateFieldValue != null) {
                if (aggregatedMap.containsKey(groupByFieldValue)) {
                    aggregatedMap.put(groupByFieldValue, aggregatedMap.get(groupByFieldValue) + aggregateFieldValue);
                } else {
                    aggregatedMap.put(groupByFieldValue, aggregateFieldValue);
                }
            }
        }

        return aggregatedMap;
    }

    public static <T extends XObject> Map<Long, List<T>> groupByIdField(List<T> coll, String fieldName) {
        Map<Long, List<T>> groupMap = new HashMap<>();
        if (coll.isEmpty() || fieldName == null) {
            return groupMap;
        }
        for (T item : coll) {
            Long groupKey = item.getAttribute(fieldName);
            if (groupMap.containsKey(groupKey)) {
                groupMap.get(groupKey).add(item);
            } else {
                List<T> list = new ArrayList<>();
                list.add(item);
                groupMap.put(groupKey, list);
            }

        }
        return groupMap;
    }


    public static <T extends XObject> Set<Long> getIdSet(List<T> objects, String idFieldName){
        Set<Long> idSet = new HashSet<>();
        if (objects.isEmpty()) {
            return idSet;
        }
        for (T o : objects) {
            Long id = o.getAttribute(idFieldName);
            if (id != null) {
                idSet.add(id);
            }
        }

        return idSet;
    }
}
