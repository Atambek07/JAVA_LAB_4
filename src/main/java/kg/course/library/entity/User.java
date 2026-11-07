package kg.course.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA-сущность «Пользователь» (таблица {@code users}, миграция V3__add.sql).
 * <p>
 * ИНН — 14-значный номер, уникален на уровне БД (CHECK: от 10<sup>14</sup> до
 * 10<sup>15</sup>−1). Хранится как {@code BIGINT}, поэтому тип поля — {@code Long}
 * (не {@code Integer}: 14-значные числа не помещаются в {@code int}).
 * </p>
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "age")
    private Integer age;

    @Column(name = "inn", nullable = false, unique = true)
    private Long inn;

    /** Конструктор без параметров для JPA (protected). */
    protected User() {
    }

    /** Открытый конструктор с проверкой аргументов. */
    public User(String name, Integer age, Long inn) {
        if (inn == null || inn < 10_000_000_000_000L || inn > 99_999_999_999_999L) {
            throw new IllegalArgumentException("ИНН должен быть 14-значным числом: " + inn);
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустой строкой");
        }
        this.name = name;
        this.age = age;
        this.inn = inn;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Long getInn() {
        return inn;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', age=" + age + ", inn=" + inn + "}";
    }
}
