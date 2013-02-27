package com.impetus.client.crud.datatypes;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.impetus.client.crud.datatypes.entities.StudentMongoBigDecimal;

public class StudentMongoBigDecimalTest extends MongoBase
{

    private static final String keyspace = "KunderaMongoDataType";

    private EntityManagerFactory emf;

    @Before
    public void setUp() throws Exception
    {
        emf = Persistence.createEntityManagerFactory("MongoDataTypeTest");
    }

    @After
    public void tearDown() throws Exception
    {
        EntityManager em = emf.createEntityManager();
        em.remove(em.find(StudentMongoBigDecimal.class, getMinValue(BigDecimal.class)));
        emf.close();
    }

    @Test
    public void testExecuteUseSameEm()
    {
        testPersist(true);
        testFindById(true);
        testMerge(true);
        testFindByQuery(true);
        testNamedQueryUseSameEm(true);
        testDelete(true);
    }

    @Test
    public void testExecute()
    {
        testPersist(false);
        testFindById(false);
        testMerge(false);
        testFindByQuery(false);
        testNamedQuery(false);
        testDelete(false);
    }

    public void testPersist(boolean useSameEm)
    {
        EntityManager em = emf.createEntityManager();

        // Insert max value of BigDecimal
        StudentMongoBigDecimal studentMax = new StudentMongoBigDecimal();
        studentMax.setAge((Short) getMaxValue(short.class));
        studentMax.setId((BigDecimal) getMaxValue(BigDecimal.class));
        studentMax.setName((String) getMaxValue(String.class));
        em.persist(studentMax);

        // Insert min value of BigDecimal
        StudentMongoBigDecimal studentMin = new StudentMongoBigDecimal();
        studentMin.setAge((Short) getMinValue(short.class));
        studentMin.setId((BigDecimal) getMinValue(BigDecimal.class));
        studentMin.setName((String) getMinValue(String.class));
        em.persist(studentMin);

        // Insert random value of BigDecimal
        StudentMongoBigDecimal student = new StudentMongoBigDecimal();
        student.setAge((Short) getRandomValue(short.class));
        student.setId((BigDecimal) getRandomValue(BigDecimal.class));
        student.setName((String) getRandomValue(String.class));
        em.persist(student);
        em.close();
    }

    public void testFindById(boolean useSameEm)
    {
        EntityManager em = emf.createEntityManager();

        StudentMongoBigDecimal studentMax = em.find(StudentMongoBigDecimal.class, getMaxValue(BigDecimal.class));
        Assert.assertNotNull(studentMax);
        Assert.assertEquals(getMaxValue(short.class), studentMax.getAge());
        Assert.assertEquals(getMaxValue(String.class), studentMax.getName());

        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        StudentMongoBigDecimal studentMin = em.find(StudentMongoBigDecimal.class, getMinValue(BigDecimal.class));
        Assert.assertNotNull(studentMin);
        Assert.assertEquals(getMinValue(short.class), studentMin.getAge());
        Assert.assertEquals(getMinValue(String.class), studentMin.getName());

        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        StudentMongoBigDecimal student = em.find(StudentMongoBigDecimal.class, getRandomValue(BigDecimal.class));
        Assert.assertNotNull(student);
        Assert.assertEquals(getRandomValue(short.class), student.getAge());
        Assert.assertEquals(getRandomValue(String.class), student.getName());
        em.close();
    }

    public void testMerge(boolean useSameEm)
    {
        EntityManager em = emf.createEntityManager();
        StudentMongoBigDecimal student = em.find(StudentMongoBigDecimal.class, getMaxValue(BigDecimal.class));
        Assert.assertNotNull(student);
        Assert.assertEquals(getMaxValue(short.class), student.getAge());
        Assert.assertEquals(getMaxValue(String.class), student.getName());

        student.setName("Kuldeep");
        em.merge(student);
        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        StudentMongoBigDecimal newStudent = em.find(StudentMongoBigDecimal.class, getMaxValue(BigDecimal.class));
        Assert.assertNotNull(newStudent);
        Assert.assertEquals(getMaxValue(short.class), newStudent.getAge());
        Assert.assertEquals("Kuldeep", newStudent.getName());
    }

    public void testFindByQuery(boolean useSameEm)
    {
        findAllQuery();
        findByName();
        findByAge();
        findByNameAndAgeGTAndLT();
        findByNameAndAgeGTEQAndLTEQ();
        findByNameAndAgeGTAndLTEQ();
        findByNameAndAgeWithOrClause();
        findByAgeAndNameGTAndLT();
        findByNameAndAGEBetween();
        findByRange();
    }

    private void findByAgeAndNameGTAndLT()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentMongoBigDecimal> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentMongoBigDecimal s where s.age = ?1 and s.name > Amresh and s.name <= ?2";
        q = em.createQuery(query);
        q.setParameter(1, getMinValue(short.class));
        q.setParameter(2, getMaxValue(String.class));
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(1, students.size());
        count = 0;
        for (StudentMongoBigDecimal student : students)
        {
            Assert.assertEquals(getMinValue(BigDecimal.class), student.getId());
            Assert.assertEquals(getMinValue(short.class), student.getAge());
            Assert.assertEquals(getMinValue(String.class), student.getName());
            count++;

        }
        Assert.assertEquals(1, count);
        em.close();

    }

    private void findByRange()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentMongoBigDecimal> students;
        em = emf.createEntityManager();
        query = "Select s From StudentMongoBigDecimal s where s.id between ?1 and ?2";
        q = em.createQuery(query);
        q.setParameter(1, getMinValue(BigDecimal.class));
        q.setParameter(2, getMaxValue(BigDecimal.class));
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(3, students.size());
        int count = 0;
        for (StudentMongoBigDecimal student : students)
        {
            if (student.getId().equals(getMaxValue(BigDecimal.class)))
            {
                Assert.assertEquals(getMaxValue(short.class), student.getAge());
                Assert.assertEquals("Kuldeep", student.getName());
                count++;
            }
            else if (student.getId().equals(getMinValue(BigDecimal.class)))
            {
                Assert.assertEquals(getMinValue(short.class), student.getAge());
                Assert.assertEquals(getMinValue(String.class), student.getName());
                count++;
            }
            else
            {
                Assert.assertEquals(getRandomValue(BigDecimal.class), student.getId());
                Assert.assertEquals(getRandomValue(short.class), student.getAge());
                Assert.assertEquals(getRandomValue(String.class), student.getName());
                count++;
            }
        }
        Assert.assertEquals(3, count);
        em.close();
    }

    private void findByNameAndAgeWithOrClause()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentMongoBigDecimal> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentMongoBigDecimal s where s.name = Kuldeep and s.age > ?1";
        q = em.createQuery(query);
        q.setParameter(1, getMinValue(short.class));
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(1, students.size());
        count = 0;
        for (StudentMongoBigDecimal student : students)
        {
            Assert.assertEquals(getMaxValue(BigDecimal.class), student.getId());
            Assert.assertEquals(getMaxValue(short.class), student.getAge());
            Assert.assertEquals("Kuldeep", student.getName());
            count++;
        }
        Assert.assertEquals(1, count);
        em.close();
    }

    private void findByNameAndAgeGTAndLTEQ()
    {

        EntityManager em;
        String query;
        Query q;
        List<StudentMongoBigDecimal> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentMongoBigDecimal s where s.name = Kuldeep and s.age > ?1 and s.age <= ?2";
        q = em.createQuery(query);
        q.setParameter(1, getMinValue(short.class));
        q.setParameter(2, getMaxValue(short.class));
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(1, students.size());
        count = 0;
        for (StudentMongoBigDecimal student : students)
        {
            Assert.assertEquals(getMaxValue(BigDecimal.class), student.getId());
            Assert.assertEquals(getMaxValue(short.class), student.getAge());
            Assert.assertEquals("Kuldeep", student.getName());
            count++;
        }
        Assert.assertEquals(1, count);
        em.close();
    }

    public void testNamedQueryUseSameEm(boolean useSameEm)
    {
        updateNamed(true);
        deleteNamed(true);
    }

    public void testNamedQuery(boolean useSameEm)
    {
        updateNamed(false);
        deleteNamed(false);
    }

    public void testDelete(boolean useSameEm)
    {
        EntityManager em = emf.createEntityManager();

        StudentMongoBigDecimal studentMax = em.find(StudentMongoBigDecimal.class, getMaxValue(BigDecimal.class));
        Assert.assertNotNull(studentMax);
        Assert.assertEquals(getMaxValue(short.class), studentMax.getAge());
        Assert.assertEquals("Kuldeep", studentMax.getName());
        em.remove(studentMax);
        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        studentMax = em.find(StudentMongoBigDecimal.class, getMaxValue(BigDecimal.class));
        Assert.assertNull(studentMax);
        em.close();
    }

    /**
     * 
     */
    private void deleteNamed(boolean useSameEm)
    {

        String deleteQuery = "Delete From StudentMongoBigDecimal s where s.name=Vivek";
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery(deleteQuery);
        q.executeUpdate();
        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        StudentMongoBigDecimal newStudent = em.find(StudentMongoBigDecimal.class, getRandomValue(BigDecimal.class));
        Assert.assertNull(newStudent);
        em.close();
    }

    /**
     * @return
     */
    private void updateNamed(boolean useSameEm)
    {
        EntityManager em = emf.createEntityManager();
        String updateQuery = "Update StudentMongoBigDecimal s SET s.name=Vivek where s.name=Amresh";
        Query q = em.createQuery(updateQuery);
        q.executeUpdate();
        if (!useSameEm)
        {
            em.close();
            em = emf.createEntityManager();
        }
        StudentMongoBigDecimal newStudent = em.find(StudentMongoBigDecimal.class, getRandomValue(BigDecimal.class));
        Assert.assertNotNull(newStudent);
        Assert.assertEquals(getRandomValue(short.class), newStudent.getAge());
        Assert.assertEquals("Vivek", newStudent.getName());
        em.close();
    }

    private void findByNameAndAGEBetween()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentMongoBigDecimal> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentMongoBigDecimal s where s.name = Amresh and s.age between ?1 and ?2";
        q = em.createQuery(query);
        q.setParameter(1, getMinValue(short.class));
        q.setParameter(2, getMaxValue(short.class));
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(1, students.size());
        count = 0;
        for (StudentMongoBigDecimal student : students)
        {
            Assert.assertEquals(getRandomValue(BigDecimal.class), student.getId());
            Assert.assertEquals(getRandomValue(short.class), student.getAge());
            Assert.assertEquals(getRandomValue(String.class), student.getName());
            count++;

        }
        Assert.assertEquals(1, count);
        em.close();
    }

    private void findByNameAndAgeGTAndLT()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentMongoBigDecimal> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentMongoBigDecimal s where s.name = Amresh and s.age > ?1 and s.age < ?2";
        q = em.createQuery(query);
        q.setParameter(1, getMinValue(short.class));
        q.setParameter(2, getMaxValue(short.class));
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(1, students.size());
        count = 0;
        for (StudentMongoBigDecimal student : students)
        {
            Assert.assertEquals(getRandomValue(BigDecimal.class), student.getId());
            Assert.assertEquals(getRandomValue(short.class), student.getAge());
            Assert.assertEquals(getRandomValue(String.class), student.getName());
            count++;

        }
        Assert.assertEquals(1, count);
        em.close();

    }

    private void findByNameAndAgeGTEQAndLTEQ()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentMongoBigDecimal> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentMongoBigDecimal s where s.name = Kuldeep and s.age >= ?1 and s.age <= ?2";
        q = em.createQuery(query);
        q.setParameter(1, getMinValue(short.class));
        q.setParameter(2, getMaxValue(short.class));
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(2, students.size());
        count = 0;
        for (StudentMongoBigDecimal student : students)
        {
            if (student.getId().equals(getMaxValue(BigDecimal.class)))
            {
                Assert.assertEquals(getMaxValue(short.class), student.getAge());
                Assert.assertEquals("Kuldeep", student.getName());
                count++;
            }
            else
            {
                Assert.assertEquals(getMinValue(BigDecimal.class), student.getId());
                Assert.assertEquals(getMinValue(short.class), student.getAge());
                Assert.assertEquals(getMinValue(String.class), student.getName());
                count++;
            }

        }
        Assert.assertEquals(2, count);
        em.close();

    }

    private void findByAge()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentMongoBigDecimal> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentMongoBigDecimal s where s.age = ?1";
        q = em.createQuery(query);
        q.setParameter(1, getRandomValue(short.class));
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(1, students.size());
        count = 0;
        for (StudentMongoBigDecimal student : students)
        {
            Assert.assertEquals(getRandomValue(BigDecimal.class), student.getId());
            Assert.assertEquals(getRandomValue(short.class), student.getAge());
            Assert.assertEquals(getRandomValue(String.class), student.getName());
            count++;
        }
        Assert.assertEquals(1, count);
        em.close();
    }

    /**
     * 
     */
    private void findByName()
    {
        EntityManager em;
        String query;
        Query q;
        List<StudentMongoBigDecimal> students;
        int count;
        em = emf.createEntityManager();
        query = "Select s From StudentMongoBigDecimal s where s.name = Kuldeep";
        q = em.createQuery(query);
        students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(2, students.size());
        count = 0;
        for (StudentMongoBigDecimal student : students)
        {
            if (student.getId().equals(getMaxValue(BigDecimal.class)))
            {
                Assert.assertEquals(getMaxValue(short.class), student.getAge());
                Assert.assertEquals("Kuldeep", student.getName());
                count++;
            }
            else
            {
                Assert.assertEquals(getMinValue(BigDecimal.class), student.getId());
                Assert.assertEquals(getMinValue(short.class), student.getAge());
                Assert.assertEquals(getMinValue(String.class), student.getName());
                count++;
            }
        }
        Assert.assertEquals(2, count);
        em.close();
    }

    /**
     * 
     */
    private void findAllQuery()
    {
        EntityManager em = emf.createEntityManager();
        // Selet all query.
        String query = "Select s From StudentMongoBigDecimal s ";
        Query q = em.createQuery(query);
        List<StudentMongoBigDecimal> students = q.getResultList();
        Assert.assertNotNull(students);
        Assert.assertEquals(3, students.size());
        int count = 0;
        for (StudentMongoBigDecimal student : students)
        {
            if (student.getId().equals(getMaxValue(BigDecimal.class)))
            {
                Assert.assertEquals(getMaxValue(short.class), student.getAge());
                Assert.assertEquals("Kuldeep", student.getName());
                count++;
            }
            else if (student.getId().equals(getMinValue(BigDecimal.class)))
            {
                Assert.assertEquals(getMinValue(short.class), student.getAge());
                Assert.assertEquals(getMinValue(String.class), student.getName());
                count++;
            }
            else
            {
                Assert.assertEquals(getRandomValue(BigDecimal.class), student.getId());
                Assert.assertEquals(getRandomValue(short.class), student.getAge());
                Assert.assertEquals(getRandomValue(String.class), student.getName());
                count++;
            }
        }
        Assert.assertEquals(3, count);
        em.close();
    }

    public void startCluster()
    {
    }

    public void stopCluster()
    {

    }

    public void createSchema()
    {
    }

    public void dropSchema()
    {
    }

}
