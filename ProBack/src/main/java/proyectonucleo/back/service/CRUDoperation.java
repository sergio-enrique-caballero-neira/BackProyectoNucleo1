package proyectonucleo.back.service;

import java.util.List;

/**
 * Contrato común para las operaciones CRUD de los servicios.
 *
 * @param <D> tipo de DTO que maneja el servicio
 */
public interface CRUDoperation<D> {

	D create(D data);

	List<D> getAll();

	D getById(Long id);

	D updateById(Long id, D data);

	void deleteById(Long id);
}
