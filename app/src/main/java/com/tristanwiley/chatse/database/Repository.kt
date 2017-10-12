package com.tristanwiley.chatse.database

/**
 * Created by jaroslawmichalik on 10.10.2017
 */
interface Repository<ID, T>{
    fun findAll() : List<T>
    fun findOne(id : ID) : T?
    fun findAllBy(spec: Specification) : List<T>
    fun insert(t: T)
    fun delete(id: ID) : Unit
    fun deleteAll() : Unit
}

interface Specification

interface Mapper<F,T>{
    fun map(f: F) : T
}