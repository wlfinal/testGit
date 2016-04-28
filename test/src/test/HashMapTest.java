package test;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class HashMapTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Map<String,String> map = new HashMap<String,String>();
		map.put("1", "11");
		
		Map<String,String> cmap = new ConcurrentHashMap<String,String>();
		for(Entry<String, String> s : map.entrySet()){
			if(s.getValue()!=null){
				cmap.put(s.getKey(), s.getValue());
			}
		}
		HashMapTest test = new HashMapTest();
		System.out.println(test.a + "/" + test.b);
		System.out.println(cmap);
		
	}
	
	String a = this.getClass().getSimpleName();
	String b = HashMapTest.class.getSimpleName();

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();
		
		try {
			//x小时
			long distance = 53;
			//注册时间
			Date date = sdf.parse("2016-04-10 12:00:00");
			
			c.setTime(date);
			c.add(Calendar.HOUR_OF_DAY, (int)distance);
			System.out.println(c.getTime());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			System.out.println("SimpleDateFormat error");
		}
		
		System.out.println(new Date().after(c.getTime()));
		
	}

	
	
}



