package com.sgv.repository;

import com.sgv.entity.Product;
import com.sgv.entity.ProductBarcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, Long> {

    @Query("SELECT pb FROM ProductBarcode pb WHERE pb.barcode = :barcode")
    Optional<ProductBarcode> findByBarcode(@Param("barcode") String barcode);

    @Query("SELECT pb FROM ProductBarcode pb WHERE LOWER(pb.barcode) = LOWER(:barcode)")
    Optional<ProductBarcode> findByBarcodeIgnoreCase(@Param("barcode") String barcode);

    List<ProductBarcode> findByProductOrderByBarcode(Product product);
}
