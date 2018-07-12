import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.xiaoniu.core.base.BaseEntity;
import com.xiaoniu.core.support.AppMessage;

import javax.persistence.OneToMany;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.*;

public class ClassifyByParam {

    public <T> AppMessage classify(List<T> list, String flag, List<String> args) {
        try {
            Class<?> aClass = list.get(0).getClass();
            String ffield = flag.substring(0, 1).toUpperCase() + flag.substring(1);
            Method classDeclaredMethod = aClass.getDeclaredMethod("get" + ffield);
            String[] alphaTable = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"};
            String[] figureTable = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
            if (list == null) {
                return AppMessage.error("1", new Object[0]);
            }

            if (list.isEmpty()) {
                return AppMessage.successdata("0", "", new Object[0]);
            }

            List jsonArray = new ArrayList();
            int length = alphaTable.length;
            List<JSONObject> finalList = new ArrayList<>();
            list.forEach(n -> finalList.add(getInvoke(n.getClass(), args, n)));
            Map<String, List<JSONObject>> map = new HashMap<>();
            Iterator<JSONObject> iterator = finalList.iterator();
            while (iterator.hasNext()) {
                JSONObject next = iterator.next();
                if (next.get(flag) != null && !next.get(flag).toString().isEmpty()) {
                    String first = this.toPinyin(next.get(flag).toString().substring(0, 1).toUpperCase());
                    if (!Arrays.asList(figureTable).contains(first) && Arrays.asList(alphaTable).contains(first)) {
                        for (int i = 0; i < length; i++) {
                            if (first.equals(alphaTable[i])) {
                                List<JSONObject> tmp;
                                if (map.containsKey(alphaTable[i])) {
                                    tmp = map.get(alphaTable[i]);
                                } else {
                                    tmp = new ArrayList<>();
                                }
                                tmp.add(next);
                                map.put(alphaTable[i], tmp);
                            }
                        }
                    } else {
                        List<JSONObject> tmp;
                        if (map.containsKey("#")) {
                            tmp = map.get("#");
                        } else {
                            tmp = new ArrayList<>();
                        }
                        tmp.add(next);
                        map.put("#", tmp);
                    }
                } else {
                    List<JSONObject> tmp;
                    if (map.containsKey("#")) {
                        tmp = map.get("#");
                    } else {
                        tmp = new ArrayList<>();
                    }
                    tmp.add(next);
                    map.put("#", tmp);
                }
                iterator.remove();
            }
            for (Map.Entry<String, List<JSONObject>> entry : map.entrySet()) {
                if (entry.getValue().size() > 0) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("firstLetter", entry.getKey());
                    jsonObject.put("list", entry.getValue());
                    jsonArray.add(jsonObject);
                }
            }
            return AppMessage.successdata("0", sortList(jsonArray, "firstLetter", 1), new Object[0]);
//
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        return AppMessage.error("2", new Object[0]);
    }

    private String toPinyin(String originHanCharacter) {
        char[] chars = originHanCharacter.trim().replaceAll(" ", "").toCharArray();
        String pinyin = "";

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] > 128) {
                try {
                    pinyin = pinyin + PinyinHelper.getShortPinyin(chars[i] + "").toUpperCase();
                } catch (PinyinException e) {
                    e.printStackTrace();
                }
            } else {
                pinyin = pinyin + chars[i];
            }
        }

        return pinyin;
    }

    private List<JSONObject> sortList(List<JSONObject> list, String flag, int sequence) {
        Collections.sort(list, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                return (o1.getString(flag).compareTo(o2.getString(flag))) * Sequence.getEnum(sequence).getCode();
            }
        });
        return list;
    }

    private <T> List<T> sortList(List<T> list, String flag) {
        try {
            if (list == null) {
                return null;
            } else if (list.isEmpty()) {
                return new ArrayList();
            } else {
                Class<?> aClass = list.get(0).getClass();
                String ffield = flag.substring(0, 1).toUpperCase() + flag.substring(1);
                final Method classDeclaredMethod = aClass.getDeclaredMethod("get" + ffield);
                Collections.sort(list, new Comparator<T>() {
                    public int compare(T o1, T o2) {
                        Method finalMethod = classDeclaredMethod;
                        Collator collator = Collator.getInstance(Locale.CHINA);

                        try {
                            if (finalMethod.invoke(o1) == null || finalMethod.invoke(o2) == null) {
                                return 0;
                            } else {
                                return collator.compare(finalMethod.invoke(o1), finalMethod.invoke(o2));
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }

                        return -1;
                    }
                });
                return list;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return new ArrayList();
        }
    }

    private <T> JSONObject getInvoke(Class aClass, List<String> args, T next) {
        try {
            JSONObject jsonObject = new JSONObject();
            for (int k = 0; k < args.size(); k++) {
                String fieldName = args.get(k);
                String getMethodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                List<String> fieldNames = new ArrayList();
                Field[] fields = aClass.getSuperclass().getDeclaredFields();
                int fieldLength = fields.length;
                Arrays.asList(fields).forEach(n -> fieldNames.add(n.getName()));
                if (fieldNames.contains(fieldName)) {
                    Object fieldValue = aClass.getSuperclass().getDeclaredMethod(getMethodName).invoke(next);
                    jsonObject.put(fieldName, fieldValue.toString());
                } else {
                    Method fieldMethod = aClass.getDeclaredMethod(getMethodName);
                    Object fieldValue = fieldMethod.invoke(next);
                    if (fieldValue != null && BaseEntity.class.isAssignableFrom(fieldValue.getClass())) {
                        JSONObject sonJSONObject = new JSONObject();
                        Field[] fields1 = fieldValue.getClass().getSuperclass().getDeclaredFields();
                        for (Field field1 : fields1) {
                            String get = "get" + field1.getName().substring(0, 1).toUpperCase() + field1.getName().substring(1);
                            Annotation[] declaredAnnotations = field1.getDeclaredAnnotations();
                            if (fieldValue.getClass().getSuperclass().getDeclaredMethod(get).invoke(fieldValue) == null) {
                                sonJSONObject.put(field1.getName(), null);
                            } else {
                                long count = Arrays.asList(declaredAnnotations).stream().filter(n -> n instanceof OneToMany).count();
                                if (count == 0 && !BaseEntity.class.isAssignableFrom(fieldValue.getClass().getSuperclass().getDeclaredMethod(get).invoke(fieldValue).getClass())) {
                                    sonJSONObject.put(field1.getName(), fieldValue.getClass().getSuperclass().getDeclaredMethod(get).invoke(fieldValue) == null ? null : fieldValue.getClass().getSuperclass().getDeclaredMethod(get).invoke(fieldValue).toString().trim());
                                }
                            }
                        }
                        jsonObject.put(fieldName, sonJSONObject);
                    } else {
                        jsonObject.put(fieldName, fieldValue == null ? null : fieldValue.toString().trim());
                    }
                }
            }
            return jsonObject;
        } catch (Exception e) {
            e.fillInStackTrace();
            return new JSONObject();
        }

    }

    private enum Sequence {
        asc("升序", 1),
        desc("降序", -1);
        private String context;
        private Integer code;

        private Sequence(String context, Integer code) {
            this.context = context;
            this.code = code;
        }

        public String getContext() {
            return context;
        }

        public Integer getCode() {
            return code;
        }

        public String getEnumName() {
            return this.getEnumName(this);
        }

        public static String getEnumName(Sequence sequence) {
            switch (sequence.getCode()) {
                case 1:
                    return Sequence.asc + "";
                case -1:
                    return Sequence.desc + "";
                default:
                    break;
            }
            return "";
        }

        public static String getContext(int status) {
            switch (status) {
                case -1:
                    return Sequence.desc.getContext();
                case 1:
                    return Sequence.asc.getContext();
                default:
                    break;
            }
            return "";
        }


        public static Sequence getEnum(int code) {
            switch (code) {
                case -1:
                    return Sequence.desc;
                case 1:
                    return Sequence.asc;
                default:
                    break;
            }
            return null;
        }

        public static int getStatus(String context) {
            switch (context) {
                case "升序":
                    return Sequence.asc.getCode();
                case "降序":
                    return Sequence.desc.getCode();
                default:
                    break;
            }
            return 0;
        }
    }
}
