import { Dinero } from '@/components/coral'

const TEXTOS = {
  nocturna: 'Se suma el cargo de recepción nocturna',
  aseo: 'Se suma el cargo de aseo por mascotas',
}

interface Props {
  tipo: keyof typeof TEXTOS
  /** Valor configurado en el alojamiento; si no viene, el aviso va sin monto */
  valor: number | null
  /** Cuando la frase va en medio de otra: "…, se suma el cargo…" */
  minuscula?: boolean
}

/** "Se suma el cargo de recepción nocturna ($X)" o, sin valor configurado, sin el monto */
export function TextoCargo({ tipo, valor, minuscula = false }: Props) {
  const texto = TEXTOS[tipo]
  return (
    <>
      {minuscula ? texto.charAt(0).toLowerCase() + texto.slice(1) : texto}
      {valor !== null && (
        <>
          {' '}
          (<Dinero valor={valor} />)
        </>
      )}
    </>
  )
}
