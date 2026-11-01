package com.mvc.nms_be;

import com.mvc.nms_be.dto.request.CategoryRequestDto;
import com.mvc.nms_be.entity.Category;
import com.mvc.nms_be.repository.CategoryRepository;
import com.mvc.nms_be.repository.NewsArticleRepository;
import com.mvc.nms_be.service.impl.CategoryServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceTests {
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final CategoryServiceImpl service = new CategoryServiceImpl(
            categories, mock(NewsArticleRepository.class));
    private Category root;
    private Category child;
    private Category grandchild;

    @BeforeEach
    void setup() {
        root = category(1, null);
        child = category(2, root);
        grandchild = category(3, child);
        when(categories.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void rejectsChildAndGrandchildAsParent() {
        for (int parentId : new int[]{2, 3}) {
            var error = assertThrows(ResponseStatusException.class,
                    () -> service.updateCategory(1, request(parentId)));
            assertEquals(400, error.getStatusCode().value());
            assertNull(root.getParentCategory());
        }
        verify(categories, never()).save(any());
    }

    @Test
    void rejectsSelfAsParent() {
        var error = assertThrows(ResponseStatusException.class,
                () -> service.updateCategory(1, request(1)));
        assertEquals(400, error.getStatusCode().value());
        verify(categories, never()).save(any());
    }

    @Test
    void rejectsParentChainThatAlreadyContainsCycle() {
        child.setParentCategory(grandchild);
        var error = assertThrows(ResponseStatusException.class,
                () -> service.updateCategory(1, request(2)));
        assertEquals(400, error.getStatusCode().value());
        verify(categories, never()).save(any());
    }

    @Test
    void allowsMovingToAnAncestor() {
        service.updateCategory(3, request(1));
        assertSame(root, grandchild.getParentCategory());
        verify(categories).save(grandchild);
    }

    @Test
    void allowsRemovingParent() {
        service.updateCategory(2, request(null));
        assertNull(child.getParentCategory());
        verify(categories).save(child);
    }

    @Test
    void allowsKeepingCurrentParent() {
        service.updateCategory(2, request(1));
        assertSame(root, child.getParentCategory());
        verify(categories).save(child);
    }

    private Category category(int id, Category parent) {
        Category result = new Category();
        result.setCategoryId(id);
        result.setCategoryName("Category " + id);
        result.setParentCategory(parent);
        when(categories.findById(id)).thenReturn(Optional.of(result));
        return result;
    }

    private CategoryRequestDto request(Integer parentId) {
        return new CategoryRequestDto("Updated", null, parentId, 1);
    }
}
