package com.amstudio.studymagic.utils;

import com.amstudio.studymagic.R;
import com.amstudio.studymagic.models.Category;
import com.amstudio.studymagic.models.Chapter;
import com.amstudio.studymagic.models.Question;
import com.amstudio.studymagic.models.Subject;
import com.amstudio.studymagic.models.Test;
import com.amstudio.studymagic.models.Topic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockData {

    public static List<Category> getCategories() {
        return Arrays.asList(
                new Category("1", "SSC Exams", R.drawable.ic_category_ssc),
                new Category("2", "Banking", R.drawable.ic_category_banking),
                new Category("3", "Railways", R.drawable.ic_category_railways),
                new Category("4", "Engineering", R.drawable.ic_category_engineering)
        );
    }

    public static List<Subject> getSubjects(String categoryId) {
        return Arrays.asList(
                new Subject("s1", "Mathematics"),
                new Subject("s2", "English Language"),
                new Subject("s3", "General Intelligence & Reasoning"),
                new Subject("s4", "General Awareness")
        );
    }

    public static List<Chapter> getChapters(String subjectId) {
        if (subjectId.equals("s1")) {
            return Arrays.asList(
                    new Chapter("c1", "Number System"),
                    new Chapter("c2", "Algebra"),
                    new Chapter("c3", "Geometry"),
                    new Chapter("c4", "Trigonometry")
            );
        }
        return Arrays.asList(
                new Chapter("c5", "Chapter 1"),
                new Chapter("c6", "Chapter 2")
        );
    }

    public static List<Topic> getTopics(String chapterId) {
        if (chapterId.equals("c3")) {
            return Arrays.asList(
                    new Topic("t1", "Triangles"),
                    new Topic("t2", "Circles"),
                    new Topic("t3", "Quadrilaterals")
            );
        }
        return Arrays.asList(
                new Topic("t4", "Topic A"),
                new Topic("t5", "Topic B")
        );
    }

    public static List<Test> getTestsForTopic(String topicId) {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("q1", "What is the capital of India?", 
                Arrays.asList("Mumbai", "New Delhi", "Kolkata", "Chennai"), 1));
        questions.add(new Question("q2", "Which planet is known as the Red Planet?", 
                Arrays.asList("Earth", "Mars", "Jupiter", "Venus"), 1));

        return Arrays.asList(
                new Test("test1", "Mock Test 01: " + topicId, "Practice questions for " + topicId, 30, questions),
                new Test("test2", "Mock Test 02: " + topicId, "Advance questions for " + topicId, 45, questions)
        );
    }

    public static List<Test> getTestsForCategory(String categoryId) {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("q1", "What is the capital of India?", 
                Arrays.asList("Mumbai", "New Delhi", "Kolkata", "Chennai"), 1));
        questions.add(new Question("q2", "Which planet is known as the Red Planet?", 
                Arrays.asList("Earth", "Mars", "Jupiter", "Venus"), 1));
        questions.add(new Question("q3", "Who wrote 'Wings of Fire'?", 
                Arrays.asList("A.P.J. Abdul Kalam", "Mahatma Gandhi", "Jawaharlal Nehru", "Subhas Chandra Bose"), 0));

        return Arrays.asList(
                new Test("t1", "SSC CGL 2024: Full Mock Test 01", "Based on latest TCS pattern with detailed solutions", 60, questions),
                new Test("t2", "IBPS PO Prelims: Quantitative Aptitude", "Practice speed math and data interpretation", 20, questions),
                new Test("t3", "RRB NTPC: General Awareness Mini", "Current affairs and static GK refresher", 15, questions)
        );
    }
}