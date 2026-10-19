package com.mvc.nms_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "Category", indexes =
        @Index(name = "IX_Category_Parent", columnList = "ParentCategoryID"))
@Check(name = "CK_Category_Active", constraints = "IsActive IN (0, 1)")
@Check(name = "CK_Category_Parent", constraints = "ParentCategoryID IS NULL OR ParentCategoryID <> CategoryID")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryID")
    private Integer categoryId;

    // nvarchar để lưu được tiếng Việt có dấu (varchar sẽ biến "Thể thao" thành "Th? thao")
    @Column(name = "CategoryName", nullable = false, length = 100, columnDefinition = "nvarchar(100)")
    private String categoryName;

    @Column(name = "CategoryDescription", length = 1000, columnDefinition = "nvarchar(1000)")
    private String categoryDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ParentCategoryID", foreignKey = @ForeignKey(name = "FK_Category_Parent"))
    private Category parentCategory;

    @Column(name = "IsActive", nullable = false)
    private Integer isActive = 1;
}
