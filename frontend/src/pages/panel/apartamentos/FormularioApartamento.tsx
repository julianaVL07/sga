import { useParams } from 'react-router-dom'
import { EnConstruccion } from '@/components/coral'

export default function FormularioApartamento() {
  const { id } = useParams()
  return <EnConstruccion titulo={id ? 'Editar apartamento' : 'Nuevo apartamento'} />
}
