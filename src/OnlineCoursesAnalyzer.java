import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class OnlineCoursesAnalyzer {

    List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
                        Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
                        Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                        Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
                        Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                        Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
                        Double.parseDouble(info[21]), Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //1
    public Map<String, Integer> getPtcpCountByInst() {
        Map<String, Integer> map = courses.stream()
                .collect(Collectors.groupingBy(Course::getInstitution,Collectors.summingInt(Course::getParticipants)));
        return new HashMap<>(map);
    }
    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Map<String, Integer> map1 = courses.stream()
                .collect(Collectors.groupingBy(new Function<Course, String>() {
                    @Override
                    public String apply(Course course) {
                        return course.getInstitution()+"-"+course.getSubject();
                    }
                }, Collectors.summingInt(Course::getParticipants)))
                .entrySet()
                .stream()
                .sorted((o1, o2) -> -o1.getValue().compareTo(o2.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(v1, v2) -> v1, LinkedHashMap::new));
        return new LinkedHashMap<>(map1);
    }
    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        Map<String, List<List<String>>> map = new HashMap<>();
        for(Course course:courses){
            String courseName = course.getTitle();
            String names = course.getInstructors();
            List<String> instructorList = new ArrayList<>(List.of(names.split(", ")));
            if(instructorList.size() == 1){
                List<List<String>> currentCourse = map.getOrDefault(instructorList.get(0), new ArrayList<>());
                if(currentCourse.size() == 0) {
                    currentCourse.add(new ArrayList<>());
                    currentCourse.add(new ArrayList<>());
                }else if(currentCourse.size() == 1){
                    currentCourse.add(new ArrayList<>());
                }
                currentCourse.get(0).add(courseName);
                List<String> distinctCurrentCourse = new ArrayList<>(currentCourse.get(0).stream().distinct().toList());
                currentCourse.set(0,distinctCurrentCourse);
                map.put(instructorList.get(0),currentCourse);
            }else{
                for(String coInstructor:instructorList){
                    List<List<String>> currentCourse = map.getOrDefault(coInstructor, new ArrayList<>());
                    if(currentCourse.size() == 0) {
                        currentCourse.add(new ArrayList<>());
                        currentCourse.add(new ArrayList<>());
                    }else if(currentCourse.size() == 1){
                        currentCourse.add(new ArrayList<>());
                    }
                    currentCourse.get(1).add(courseName);
                    List<String> distinctCurrentCourse = new ArrayList<>(currentCourse.get(1).stream().distinct().toList());
                    currentCourse.set(1,distinctCurrentCourse);
                    map.put(coInstructor,currentCourse);
                }
            }
        }
        map.forEach((k, v) -> v.forEach(Collections::sort));
        return map;
    }

    //4
    public List<String> getCourses(int topK, String by) {
        List<Course>courseList = new ArrayList<>(courses);
        if (by.equals("hours")) {
            courseList.sort(Comparator.comparing(Course::getTotalHours).reversed().thenComparing(Course::getTitle));
        } else{
            courseList.sort(Comparator.comparing(Course::getParticipants).reversed().thenComparing(Course::getTitle));
        }
        List<String> topKCourses = new ArrayList<>();
        for(Course course:courseList){
            if(!topKCourses.contains(course.getTitle())){
                topKCourses.add(course.getTitle());
                if(topKCourses.size() == topK){
                    break;
                }
            }
        }
        return topKCourses;
    }

    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        List<String>courseList = new ArrayList<>();
        for (Course course:courses) {
            if (course.getSubject().toLowerCase().contains(courseSubject.toLowerCase()) && course.getPercentAudited() >= percentAudited
                    && course.getTotalHours() <= totalCourseHours) {
                courseList.add(course.getTitle());
            }
        }
        List<String> newCourseList= new ArrayList<>(courseList.stream().distinct().toList());
        Collections.sort(newCourseList);
        return newCourseList;
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        List<String>recommendCoursesList = new ArrayList<>();
        Map<String, Double> averageMedianAge = courses.stream().collect(Collectors.groupingBy(Course::getNumber, Collectors.averagingDouble(Course::getMedianAge)));
        Map<String, Double> avgMale = courses.stream().collect(Collectors.groupingBy(Course::getNumber,Collectors.averagingDouble(Course::getPercentMale)));
        Map<String, Double> avgDegree = courses.stream().collect(Collectors.groupingBy(Course::getNumber,Collectors.averagingDouble(Course::getPercentDegree)));
        for(Course course:courses){
            double similarityValue = Math.pow(age-averageMedianAge.get(course.getNumber()) ,2) + Math.pow((100 * gender - avgMale.get(course.getNumber())),2)
                    + Math.pow((100 * isBachelorOrHigher - avgDegree.get(course.getNumber())),2);
            course.setSimilarity(similarityValue);
        }
//        courses.sort(Comparator.comparing(Course::getSimilarity));
        List<Course> sortedCourse = courses.stream()
                .collect(Collectors.groupingBy(Course::getNumber, Collectors.maxBy(Comparator.comparing(Course::getLaunchDate))))
                .values()
                .stream()
                .map(Optional::get)
                .sorted(Comparator.comparing(Course::getSimilarity).thenComparing(Course::getTitle)).toList();
        for(Course course: sortedCourse){
            if(recommendCoursesList.size() == 10){
                break;
            }
            if(!recommendCoursesList.contains(course.getTitle())){
                recommendCoursesList.add(course.getTitle());
            }
        }
        return recommendCoursesList;
    }

}

class Course {
    String institution;
    String number;
    Date launchDate;
    String title;
    String instructors;
    String subject;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    double similarity;

    public Course(String institution, String number, Date launchDate,
                  String title, String instructors, String subject,
                  int year, int honorCode, int participants,
                  int audited, int certified, double percentAudited,
                  double percentCertified, double percentCertified50,
                  double percentVideo, double percentForum, double gradeHigherZero,
                  double totalHours, double medianHoursCertification,
                  double medianAge, double percentMale, double percentFemale,
                  double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) title = title.substring(1);
        if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
        this.title = title;
        if (instructors.startsWith("\"")) instructors = instructors.substring(1);
        if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
        this.instructors = instructors;
        if (subject.startsWith("\"")) subject = subject.substring(1);
        if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
        this.subject = subject;
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Date getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(Date launchDate) {
        this.launchDate = launchDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructors() {
        return instructors;
    }

    public void setInstructors(String instructors) {
        this.instructors = instructors;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getHonorCode() {
        return honorCode;
    }

    public void setHonorCode(int honorCode) {
        this.honorCode = honorCode;
    }

    public int getParticipants() {
        return participants;
    }

    public void setParticipants(int participants) {
        this.participants = participants;
    }

    public int getAudited() {
        return audited;
    }

    public void setAudited(int audited) {
        this.audited = audited;
    }

    public int getCertified() {
        return certified;
    }

    public void setCertified(int certified) {
        this.certified = certified;
    }

    public double getPercentAudited() {
        return percentAudited;
    }

    public void setPercentAudited(double percentAudited) {
        this.percentAudited = percentAudited;
    }

    public double getPercentCertified() {
        return percentCertified;
    }

    public void setPercentCertified(double percentCertified) {
        this.percentCertified = percentCertified;
    }

    public double getPercentCertified50() {
        return percentCertified50;
    }

    public void setPercentCertified50(double percentCertified50) {
        this.percentCertified50 = percentCertified50;
    }

    public double getPercentVideo() {
        return percentVideo;
    }

    public void setPercentVideo(double percentVideo) {
        this.percentVideo = percentVideo;
    }

    public double getPercentForum() {
        return percentForum;
    }

    public void setPercentForum(double percentForum) {
        this.percentForum = percentForum;
    }

    public double getGradeHigherZero() {
        return gradeHigherZero;
    }

    public void setGradeHigherZero(double gradeHigherZero) {
        this.gradeHigherZero = gradeHigherZero;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(double totalHours) {
        this.totalHours = totalHours;
    }

    public double getMedianHoursCertification() {
        return medianHoursCertification;
    }

    public void setMedianHoursCertification(double medianHoursCertification) {
        this.medianHoursCertification = medianHoursCertification;
    }

    public double getMedianAge() {
        return medianAge;
    }

    public void setMedianAge(double medianAge) {
        this.medianAge = medianAge;
    }

    public double getPercentMale() {
        return percentMale;
    }

    public void setPercentMale(double percentMale) {
        this.percentMale = percentMale;
    }

    public double getPercentFemale() {
        return percentFemale;
    }

    public void setPercentFemale(double percentFemale) {
        this.percentFemale = percentFemale;
    }

    public double getPercentDegree() {
        return percentDegree;
    }

    public void setPercentDegree(double percentDegree) {
        this.percentDegree = percentDegree;
    }
}
