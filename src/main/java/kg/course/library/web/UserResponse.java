package kg.course.library.web;

import kg.course.library.entity.User;

/**
 * Ответ API, представляющий пользователя.
 */
public record UserResponse(
        long id,
        String name,
        Integer age,
        Long inn
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getAge(), user.getInn());
    }
}
